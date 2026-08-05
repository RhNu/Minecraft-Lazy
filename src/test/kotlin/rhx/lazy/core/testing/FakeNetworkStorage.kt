package rhx.lazy.core.testing

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.io.IoResourceKind
import rhx.lazy.core.io.NetworkOutputProvider
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTargetResolution
import rhx.lazy.core.io.NetworkTransferResult
import rhx.lazy.core.storage.NetworkStorageId
import rhx.lazy.core.storage.NetworkStoragePort
import rhx.lazy.core.storage.NetworkStorageResult
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

internal class FakeNetworkStorage(
    override var isAvailable: Boolean = true,
) : NetworkStoragePort {
    var networkExists = true
    var itemCapacity = Long.MAX_VALUE
    var fluidCapacity = Long.MAX_VALUE
    var energyCapacity = Long.MAX_VALUE

    var storedItem = ItemStack.EMPTY
    var storedItemAmount = 0L
    var storedFluid = FluidStack.EMPTY
    var storedFluidAmount = 0L
    var storedEnergy = 0L

    override fun primaryNetwork(player: ServerPlayer): NetworkStorageResult<NetworkStorageId> =
        if (networkExists) {
            NetworkStorageResult.Success(TEST_NETWORK_ID)
        } else {
            NetworkStorageResult.NetworkNotFound
        }

    override fun itemAmount(
        networkId: NetworkStorageId,
        stack: ItemStack,
    ): NetworkStorageResult<Long> = withNetwork { storedItemAmount }

    override fun insertItem(
        networkId: NetworkStorageId,
        stack: ItemStack,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack> =
        withNetwork {
            val accepted = min(stack.count.toLong(), (itemCapacity - storedItemAmount).coerceAtLeast(0L))
            if (!simulate && accepted > 0L) {
                storedItem = stack.copyWithCount(1)
                storedItemAmount += accepted
            }
            stack.copyWithAmount(stack.count.toLong() - accepted)
        }

    override fun insertItemAmount(
        networkId: NetworkStorageId,
        template: ItemStack,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> =
        withNetwork {
            if (template.isEmpty || amount <= 0L) return@withNetwork 0L
            val accepted = min(amount, (itemCapacity - storedItemAmount).coerceAtLeast(0L))
            if (!simulate) storedItemAmount += accepted
            amount - accepted
        }

    override fun extractItem(
        networkId: NetworkStorageId,
        template: ItemStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<ItemStack> =
        withNetwork {
            val extracted = min(amount.toLong().coerceAtLeast(0L), storedItemAmount)
            if (!simulate) storedItemAmount -= extracted
            template.copyWithAmount(extracted)
        }

    override fun fluidAmount(
        networkId: NetworkStorageId,
        stack: FluidStack,
    ): NetworkStorageResult<Long> = withNetwork { storedFluidAmount }

    override fun insertFluid(
        networkId: NetworkStorageId,
        stack: FluidStack,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack> =
        withNetwork {
            val accepted = min(stack.amount.toLong(), (fluidCapacity - storedFluidAmount).coerceAtLeast(0L))
            if (!simulate && accepted > 0L) {
                storedFluid = stack.copyWithAmount(1)
                storedFluidAmount += accepted
            }
            stack.copyWithAmount(stack.amount.toLong() - accepted)
        }

    override fun extractFluid(
        networkId: NetworkStorageId,
        template: FluidStack,
        amount: Int,
        simulate: Boolean,
    ): NetworkStorageResult<FluidStack> =
        withNetwork {
            val extracted = min(amount.toLong().coerceAtLeast(0L), storedFluidAmount)
            if (!simulate) storedFluidAmount -= extracted
            template.copyWithAmount(extracted)
        }

    override fun energyAmount(networkId: NetworkStorageId): NetworkStorageResult<Long> = withNetwork { storedEnergy }

    override fun insertEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> =
        withNetwork {
            val accepted = min(amount.coerceAtLeast(0L), (energyCapacity - storedEnergy).coerceAtLeast(0L))
            if (!simulate) storedEnergy += accepted
            amount.coerceAtLeast(0L) - accepted
        }

    override fun extractEnergy(
        networkId: NetworkStorageId,
        amount: Long,
        simulate: Boolean,
    ): NetworkStorageResult<Long> =
        withNetwork {
            val extracted = min(amount.coerceAtLeast(0L), storedEnergy)
            if (!simulate) storedEnergy -= extracted
            extracted
        }

    private fun <T> withNetwork(value: () -> T): NetworkStorageResult<T> =
        when {
            !isAvailable -> NetworkStorageResult.Unavailable
            !networkExists -> NetworkStorageResult.NetworkNotFound
            else -> NetworkStorageResult.Success(value())
        }

    companion object {
        val TEST_NETWORK_ID = NetworkStorageId(7)
    }
}

internal class FakeNetworkOutputProvider(
    private val storage: FakeNetworkStorage,
    override val supportedResourceKinds: Set<IoResourceKind> = IoResourceKind.entries.toSet(),
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
        when (val result = storage.primaryNetwork(player)) {
            is NetworkStorageResult.Success -> NetworkTargetResolution.Success(target.copy())
            NetworkStorageResult.NetworkNotFound -> NetworkTargetResolution.NotFound
            NetworkStorageResult.Unavailable -> NetworkTargetResolution.Unavailable
            else -> NetworkTargetResolution.Failed
        }

    override fun isTargetValid(target: NetworkTargetRef): Boolean = networkId(target) != null

    override fun insert(
        target: NetworkTargetRef,
        payload: NetworkPayload,
        simulate: Boolean,
    ): NetworkTransferResult {
        val networkId = networkId(target) ?: return NetworkTransferResult.TargetMissing
        val result =
            when (payload) {
                is NetworkPayload.Items -> storage.insertItemAmount(networkId, payload.template, payload.amount, simulate)
                is NetworkPayload.Fluid -> storage.insertFluid(networkId, payload.stack, simulate)
                is NetworkPayload.Energy -> storage.insertEnergy(networkId, payload.amount, simulate)
            }
        return when (result) {
            is NetworkStorageResult.Success<*> ->
                when (val value = result.value) {
                    is Long -> NetworkTransferResult.Success(value)
                    is FluidStack -> NetworkTransferResult.Success(value.amount.toLong())
                    else -> error("Unexpected fake network result: $value")
                }

            NetworkStorageResult.NetworkNotFound -> NetworkTransferResult.TargetMissing
            NetworkStorageResult.OutcomeUnknown -> NetworkTransferResult.OutcomeUnknown
            else -> NetworkTransferResult.TemporarilyUnavailable
        }
    }

    private fun networkId(target: NetworkTargetRef): NetworkStorageId? {
        if (target.providerId != id || !target.data.contains(NETWORK_ID_TAG, Tag.TAG_INT.toInt())) return null
        return target.data
            .getInt(NETWORK_ID_TAG)
            .takeIf { it >= 0 }
            ?.let(::NetworkStorageId)
    }

    private companion object {
        const val NETWORK_ID_TAG = "networkId"
        val nextId = AtomicInteger()
    }
}

private fun ItemStack.copyWithAmount(amount: Long): ItemStack =
    if (amount <= 0L) ItemStack.EMPTY else copyWithCount(amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())

private fun FluidStack.copyWithAmount(amount: Long): FluidStack =
    if (amount <= 0L) FluidStack.EMPTY else copyWithAmount(amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
