package rhx.lazy.core.testing

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.io.NetworkOutputProvider
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTargetResolution
import rhx.lazy.core.io.ResourceKinds
import rhx.lazy.core.io.TransferResult
import rhx.lazy.core.resource.EnergyVariant
import rhx.lazy.core.resource.FluidVariant
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceVariant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

internal class FakeNetworkStorage(
    var isAvailable: Boolean = true,
) {
    var networkExists = true
    var outcomeUnknown = false
    var itemCapacity = Long.MAX_VALUE
    var fluidCapacity = Long.MAX_VALUE
    var energyCapacity = Long.MAX_VALUE

    var storedItem = ItemStack.EMPTY
    var storedItemAmount = 0L
    var storedFluid = FluidStack.EMPTY
    var storedFluidAmount = 0L
    var storedEnergy = 0L

    fun insertItemAmount(
        template: ItemStack,
        amount: Long,
        simulate: Boolean,
    ): TransferResult =
        transfer {
            if (template.isEmpty || amount <= 0L) return@transfer 0L
            val accepted = min(amount, (itemCapacity - storedItemAmount).coerceAtLeast(0L))
            if (!simulate && accepted > 0) {
                storedItem = template.copyWithCount(1)
                storedItemAmount += accepted
            }
            accepted
        }

    fun insertFluid(
        template: FluidStack,
        amount: Long,
        simulate: Boolean,
    ): TransferResult =
        transfer {
            val accepted = min(amount, (fluidCapacity - storedFluidAmount).coerceAtLeast(0L))
            if (!simulate && accepted > 0) {
                storedFluid = template.copyWithAmount(1)
                storedFluidAmount += accepted
            }
            accepted
        }

    fun insertEnergy(
        amount: Long,
        simulate: Boolean,
    ): TransferResult =
        transfer {
            val requested = amount.coerceAtLeast(0L)
            val accepted = min(requested, (energyCapacity - storedEnergy).coerceAtLeast(0L))
            if (!simulate) storedEnergy += accepted
            accepted
        }

    private fun transfer(operation: () -> Long): TransferResult =
        when {
            outcomeUnknown -> TransferResult.OutcomeUnknown
            !isAvailable -> TransferResult.TemporarilyUnavailable
            !networkExists -> TransferResult.TargetMissing
            else -> TransferResult.Accepted(operation())
        }

    companion object {
        val TEST_NETWORK_ID = FakeNetworkId(7)
    }
}

@JvmInline
internal value class FakeNetworkId(
    val value: Int,
)

internal class FakeNetworkOutputProvider(
    private val storage: FakeNetworkStorage,
    override val capabilities: Set<ResourceKind<out ResourceVariant>> = ResourceKinds.all,
) : NetworkOutputProvider {
    override val id: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath("lazy", "test_network_${nextId.getAndIncrement()}")
    override val displayName: Component = Component.literal("Test network")

    val target: NetworkTargetRef =
        NetworkTargetRef(
            id,
            CompoundTag().apply { putInt(NETWORK_ID_TAG, FakeNetworkStorage.TEST_NETWORK_ID.value) },
        )

    init {
        NetworkOutputProviders.register(this)
    }

    override fun icon(): ItemStack = ItemStack(Items.CHEST)

    override fun resolvePrimaryTarget(player: ServerPlayer): NetworkTargetResolution =
        when {
            !storage.isAvailable -> NetworkTargetResolution.Unavailable
            !storage.networkExists -> NetworkTargetResolution.NotFound
            else -> NetworkTargetResolution.Success(target.copy())
        }

    override fun isTargetValid(target: NetworkTargetRef): Boolean = networkId(target) != null

    override fun offer(
        target: NetworkTargetRef,
        amount: ResourceAmount<out ResourceVariant>,
        simulate: Boolean,
    ): TransferResult {
        if (networkId(target) == null) return TransferResult.InvalidTarget
        return when (val variant = amount.variant) {
            is ItemVariant -> storage.insertItemAmount(variant.template, amount.amount, simulate)
            is FluidVariant -> storage.insertFluid(variant.template, amount.amount, simulate)
            EnergyVariant -> storage.insertEnergy(amount.amount, simulate)
        }
    }

    private fun networkId(target: NetworkTargetRef): FakeNetworkId? {
        if (target.providerId != id || !target.data.contains(NETWORK_ID_TAG, Tag.TAG_INT.toInt())) return null
        return target.data
            .getInt(NETWORK_ID_TAG)
            .takeIf { it >= 0 }
            ?.let(::FakeNetworkId)
    }

    private companion object {
        const val NETWORK_ID_TAG = "networkId"
        val nextId = AtomicInteger()
    }
}
