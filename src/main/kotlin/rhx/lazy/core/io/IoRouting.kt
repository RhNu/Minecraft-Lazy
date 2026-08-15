package rhx.lazy.core.io

import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

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

/**
 * Opaque handle to a provider-specific output destination.
 *
 * [data] is owned by the configuration that holds it; providers must treat it as read-only. Use
 * [deepCopy] wherever a target crosses an ownership boundary.
 */
internal data class NetworkTargetRef(
    val providerId: ResourceLocation,
    val data: CompoundTag,
) {
    fun deepCopy(): NetworkTargetRef = NetworkTargetRef(providerId, data.copy())

    fun save(): CompoundTag =
        CompoundTag().apply {
            putString(PROVIDER_TAG, providerId.toString())
            put(DATA_TAG, data.copy())
        }

    companion object {
        fun load(tag: CompoundTag): NetworkTargetRef? {
            val provider = ResourceLocation.tryParse(tag.getString(PROVIDER_TAG)) ?: return null
            return NetworkTargetRef(provider, tag.getCompound(DATA_TAG).copy())
        }

        private const val PROVIDER_TAG = "provider"
        private const val DATA_TAG = "data"
    }
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
    private var snapshot = emptyList<NetworkOutputProvider>()

    fun register(provider: NetworkOutputProvider) {
        check(providers.putIfAbsent(provider.id, provider) == null) {
            "A network output provider is already registered for ${provider.id}"
        }
        snapshot = providers.values.toList()
    }

    fun all(): List<NetworkOutputProvider> = snapshot

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

internal fun NetworkTransferResult.toPushResult(): IoPushResult =
    when (this) {
        is NetworkTransferResult.Success -> IoPushResult.Success
        NetworkTransferResult.TargetMissing, NetworkTransferResult.InvalidTarget -> IoPushResult.TargetMissing
        NetworkTransferResult.OutcomeUnknown -> IoPushResult.OutcomeUnknown
        NetworkTransferResult.TemporarilyUnavailable -> IoPushResult.Retry
    }

/** Outcome of offering a single payload to a network target. */
internal sealed interface NetworkOffer {
    /** [accepted] is always clamped to the offered amount. */
    data class Accepted(
        val accepted: Long,
    ) : NetworkOffer

    data class Rejected(
        val push: IoPushResult,
    ) : NetworkOffer
}

/**
 * Sends [payload] to this target and reports how much of [offered] was taken, so callers only deal
 * with accepted amounts instead of re-deriving them from remainders.
 */
internal fun NetworkTargetRef.offer(
    payload: NetworkPayload,
    offered: Long,
): NetworkOffer =
    when (val result = NetworkOutputRouter.insert(this, payload, false)) {
        is NetworkTransferResult.Success -> NetworkOffer.Accepted(offered - result.remainder.coerceIn(0L, offered))
        else -> NetworkOffer.Rejected(result.toPushResult())
    }

/**
 * Machine-side half of the IO system.
 *
 * [IoController] owns mode dispatch, retry back-off and pause handling; adapters only describe how
 * their machine moves resources. Every hook is called on the server thread only.
 */
internal interface IoAdapter {
    val capabilities: Set<NetworkInsertCapability>

    /** Machines that never accept an inbound transfer skip the input face states while cycling. */
    val acceptsInput: Boolean
        get() = true

    /** Adapters holding buffered output that must drain even without an active push target. */
    val maintainsWhenIdle: Boolean
        get() = false

    fun supportsNetworkTarget(target: NetworkTargetRef): Boolean =
        NetworkOutputProviders
            .get(target.providerId)
            ?.let { provider -> capabilities.any { it in provider.capabilities } && provider.isTargetValid(target) }
            ?: false

    /** Local upkeep; runs every tick in passive mode and before every face push. */
    fun maintain() = Unit

    /** Rate limit or precondition, evaluated once per tick immediately before a push. */
    fun readyToPush(): Boolean = true

    /** [directions] is never empty. */
    fun pushToFaces(directions: Set<Direction>): IoPushResult = IoPushResult.Success

    fun pushToNetwork(target: NetworkTargetRef): IoPushResult = IoPushResult.Success
}
