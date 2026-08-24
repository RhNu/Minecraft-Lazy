package rhx.lazy.core.io

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.resource.EnergyResourceKind
import rhx.lazy.core.resource.FluidResourceKind
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceVariant

internal object ResourceKinds {
    val ITEM = ItemResourceKind
    val FLUID = FluidResourceKind
    val ENERGY = EnergyResourceKind
    val all: Set<ResourceKind<out ResourceVariant>> = setOf(ITEM, FLUID, ENERGY)
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

internal sealed interface TransferResult {
    /** [accepted] is always clamped to the offered amount. */
    data class Accepted(
        val accepted: Long,
    ) : TransferResult

    data object TemporarilyUnavailable : TransferResult

    data object TargetMissing : TransferResult

    data object InvalidTarget : TransferResult

    data object OutcomeUnknown : TransferResult
}

internal interface NetworkOutputProvider {
    val id: ResourceLocation
    val displayName: Component
    val capabilities: Set<ResourceKind<out ResourceVariant>>

    fun icon(): ItemStack

    fun resolvePrimaryTarget(player: ServerPlayer): NetworkTargetResolution

    fun isTargetValid(target: NetworkTargetRef): Boolean

    /** Implementations must treat [target] and its opaque data as read-only. */
    fun offer(
        target: NetworkTargetRef,
        amount: ResourceAmount<out ResourceVariant>,
        simulate: Boolean,
    ): TransferResult
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
    fun offer(
        target: NetworkTargetRef,
        amount: ResourceAmount<out ResourceVariant>,
        simulate: Boolean,
    ): TransferResult {
        val provider = NetworkOutputProviders.get(target.providerId) ?: return TransferResult.TemporarilyUnavailable
        if (amount.kind !in provider.capabilities) return TransferResult.TemporarilyUnavailable
        if (!provider.isTargetValid(target)) return TransferResult.InvalidTarget
        return when (val result = provider.offer(target, amount, simulate)) {
            is TransferResult.Accepted -> TransferResult.Accepted(result.accepted.coerceIn(0L, amount.amount))
            else -> result
        }
    }
}

internal sealed interface IoPushResult {
    data object Success : IoPushResult

    data object Retry : IoPushResult

    data object TargetMissing : IoPushResult

    data object OutcomeUnknown : IoPushResult
}

internal fun NetworkTargetRef.offer(amount: ResourceAmount<out ResourceVariant>): TransferResult =
    NetworkOutputRouter.offer(this, amount, false)

/**
 * Machine-side half of the IO system.
 *
 * [IoController] owns mode dispatch, retry back-off and pause handling; adapters only describe how
 * their machine moves resources. Every hook is called on the server thread only.
 */
internal interface IoAdapter {
    val outputSource: OutputSource

    val capabilities: Set<ResourceKind<out ResourceVariant>>
        get() = outputSource.capabilities

    /** Machines that never accept an inbound transfer skip the input face states while cycling. */
    val acceptsInput: Boolean
        get() = true

    fun supportsNetworkTarget(target: NetworkTargetRef): Boolean =
        NetworkOutputProviders
            .get(target.providerId)
            ?.let { provider -> capabilities.any { it in provider.capabilities } && provider.isTargetValid(target) }
            ?: false

    /** Rate limit or precondition, evaluated once per tick immediately before a push. */
    fun readyToPush(): Boolean = true
}
