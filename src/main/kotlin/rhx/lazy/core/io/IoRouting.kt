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

internal enum class IoResourceKind {
    ITEM,
    FLUID,
    ENERGY,
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

    data object Failed : NetworkTargetResolution
}

internal sealed interface NetworkPayload {
    data class Items(
        val template: ItemStack,
        val amount: Long,
    ) : NetworkPayload

    data class Fluid(
        val stack: FluidStack,
    ) : NetworkPayload

    data class Energy(
        val amount: Long,
    ) : NetworkPayload
}

internal sealed interface NetworkTransferResult {
    data class Success(
        val remainder: Long,
    ) : NetworkTransferResult

    data object TemporarilyUnavailable : NetworkTransferResult

    data object TargetMissing : NetworkTransferResult

    data object OutcomeUnknown : NetworkTransferResult
}

internal interface NetworkOutputProvider {
    val id: ResourceLocation
    val displayName: Component
    val supportedResourceKinds: Set<IoResourceKind>

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

internal sealed interface IoPushResult {
    data object Success : IoPushResult

    data object Retry : IoPushResult

    data object TargetMissing : IoPushResult

    data object OutcomeUnknown : IoPushResult
}

internal interface IoRouteAdapter {
    val supportedRoutes: Set<IoRoute>
    val resourceKinds: Set<IoResourceKind>
    val ticksWhenPassive: Boolean
        get() = false

    fun supportsNetworkTarget(target: NetworkTargetRef): Boolean =
        NetworkOutputProviders
            .get(target.providerId)
            ?.let { provider -> resourceKinds.any { it in provider.supportedResourceKinds } && provider.isTargetValid(target) }
            ?: false

    fun push(
        route: IoRoute,
        target: NetworkTargetRef?,
    ): IoPushResult
}
