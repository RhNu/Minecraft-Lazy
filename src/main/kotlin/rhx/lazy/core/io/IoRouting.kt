package rhx.lazy.core.io

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

internal enum class IoRoute {
    PASSIVE,
    DOWNWARD,
    ADJACENT,
    NETWORK,
}

internal class NetworkInsertCapability(
    val id: ResourceLocation,
    val displayName: Component,
) {
    override fun equals(other: Any?): Boolean = other is NetworkInsertCapability && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id.toString()
}

internal object NetworkInsertCapabilities {
    val ITEM =
        NetworkInsertCapability(
            ResourceLocation.fromNamespaceAndPath("lazy", "item"),
            Component.translatable("gui.lazy.io.capability.item"),
        )
    val FLUID =
        NetworkInsertCapability(
            ResourceLocation.fromNamespaceAndPath("lazy", "fluid"),
            Component.translatable("gui.lazy.io.capability.fluid"),
        )
    val ENERGY =
        NetworkInsertCapability(
            ResourceLocation.fromNamespaceAndPath("neoforge", "energy"),
            Component.translatable("gui.lazy.io.capability.energy"),
        )

    val all: Set<NetworkInsertCapability> = setOf(ITEM, FLUID, ENERGY)
}

internal data class NetworkTargetRef(
    val providerId: ResourceLocation,
    val data: CompoundTag,
) {
    fun copy(): NetworkTargetRef = NetworkTargetRef(providerId, data.copy())
}

internal sealed interface NetworkTargetResolution {
    data class Success(
        val target: NetworkTargetRef,
    ) : NetworkTargetResolution

    data object Unavailable : NetworkTargetResolution

    data object NotFound : NetworkTargetResolution

    data object Unlinked : NetworkTargetResolution

    data object Ambiguous : NetworkTargetResolution

    data object Incompatible : NetworkTargetResolution

    data object Failed : NetworkTargetResolution
}

internal sealed interface NetworkPayload {
    val capability: NetworkInsertCapability

    data class Items(
        val template: ItemStack,
        val amount: Long,
    ) : NetworkPayload {
        override val capability: NetworkInsertCapability = NetworkInsertCapabilities.ITEM
    }

    data class Fluid(
        val stack: FluidStack,
    ) : NetworkPayload {
        override val capability: NetworkInsertCapability = NetworkInsertCapabilities.FLUID
    }

    data class Energy(
        val amount: Long,
    ) : NetworkPayload {
        override val capability: NetworkInsertCapability = NetworkInsertCapabilities.ENERGY
    }
}

internal sealed interface NetworkTransferResult {
    data class Success(
        val remainder: Long,
    ) : NetworkTransferResult

    data object TemporarilyUnavailable : NetworkTransferResult

    data object TargetMissing : NetworkTransferResult

    data object InvalidTarget : NetworkTransferResult

    data object OutcomeUnknown : NetworkTransferResult
}

internal interface NetworkOutputProvider {
    val id: ResourceLocation
    val displayName: Component
    val capabilities: Set<NetworkInsertCapability>

    fun icon(): ItemStack

    fun resolvePrimaryTarget(player: ServerPlayer): NetworkTargetResolution

    fun isTargetValid(target: NetworkTargetRef): Boolean

    /** Implementations must treat [target] and its opaque data as read-only. */
    fun insert(
        target: NetworkTargetRef,
        payload: NetworkPayload,
        simulate: Boolean,
    ): NetworkTransferResult
}

internal object NetworkOutputProviders {
    private val providers = linkedMapOf<ResourceLocation, NetworkOutputProvider>()

    fun register(provider: NetworkOutputProvider) {
        check(providers.putIfAbsent(provider.id, provider) == null) {
            "A network output provider is already registered for ${provider.id}"
        }
    }

    fun all(): List<NetworkOutputProvider> = providers.values.toList()

    fun get(id: ResourceLocation): NetworkOutputProvider? = providers[id]
}

internal object NetworkOutputRouter {
    fun insert(
        target: NetworkTargetRef,
        payload: NetworkPayload,
        simulate: Boolean,
    ): NetworkTransferResult {
        val provider = NetworkOutputProviders.get(target.providerId) ?: return NetworkTransferResult.TemporarilyUnavailable
        if (payload.capability !in provider.capabilities) return NetworkTransferResult.TemporarilyUnavailable
        if (!provider.isTargetValid(target)) return NetworkTransferResult.InvalidTarget
        return provider.insert(target, payload, simulate)
    }
}

internal sealed interface IoPushResult {
    data object Success : IoPushResult

    data object Retry : IoPushResult

    data object TargetMissing : IoPushResult

    data object OutcomeUnknown : IoPushResult
}

internal interface IoRouteAdapter {
    val supportedRoutes: Set<IoRoute>
    val capabilities: Set<NetworkInsertCapability>
    val ticksWhenPassive: Boolean
        get() = false

    fun supportsNetworkTarget(target: NetworkTargetRef): Boolean =
        NetworkOutputProviders
            .get(target.providerId)
            ?.let { provider -> capabilities.any { it in provider.capabilities } && provider.isTargetValid(target) }
            ?: false

    fun push(
        route: IoRoute,
        target: NetworkTargetRef?,
    ): IoPushResult
}
