package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.neoforged.neoforge.energy.IEnergyStorage
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemHandlerHelper
import rhx.lazy.core.resource.EnergyResourceKind
import rhx.lazy.core.resource.EnergyVariant
import rhx.lazy.core.resource.FluidResourceKind
import rhx.lazy.core.resource.FluidVariant
import rhx.lazy.core.resource.ItemResourceKind
import rhx.lazy.core.resource.ItemVariant
import java.util.function.BooleanSupplier
import kotlin.math.min

internal object BuiltInResourceFaceTransfers {
    init {
        ResourceFaceTransferFactories.register(ItemFaceTransferFactory)
        ResourceFaceTransferFactories.register(FluidFaceTransferFactory)
        ResourceFaceTransferFactories.register(EnergyFaceTransferFactory)
    }

    fun install() = Unit
}

private object ItemFaceTransferFactory : ResourceFaceTransferFactory<ItemVariant> {
    override val kind = ItemResourceKind

    override fun create(
        origin: BlockPos,
        stillValid: BooleanSupplier,
    ): ResourceFaceTransfer<ItemVariant> {
        val targets = NeighborCapabilities.items(origin, stillValid::getAsBoolean)
        return object : ResourceFaceTransfer<ItemVariant> {
            override fun offer(
                level: ServerLevel,
                direction: Direction,
                variant: ItemVariant,
                amount: Long,
            ): Long = offerItem(targets[level, direction], variant, amount)

            override fun invalidate() = targets.invalidate()
        }
    }
}

private object FluidFaceTransferFactory : ResourceFaceTransferFactory<FluidVariant> {
    override val kind = FluidResourceKind

    override fun create(
        origin: BlockPos,
        stillValid: BooleanSupplier,
    ): ResourceFaceTransfer<FluidVariant> {
        val targets = NeighborCapabilities.fluids(origin, stillValid::getAsBoolean)
        return object : ResourceFaceTransfer<FluidVariant> {
            override fun offer(
                level: ServerLevel,
                direction: Direction,
                variant: FluidVariant,
                amount: Long,
            ): Long = offerFluid(targets[level, direction], variant, amount)

            override fun invalidate() = targets.invalidate()
        }
    }
}

private object EnergyFaceTransferFactory : ResourceFaceTransferFactory<EnergyVariant> {
    override val kind = EnergyResourceKind

    override fun create(
        origin: BlockPos,
        stillValid: BooleanSupplier,
    ): ResourceFaceTransfer<EnergyVariant> {
        val targets = NeighborCapabilities.energy(origin, stillValid::getAsBoolean)
        return object : ResourceFaceTransfer<EnergyVariant> {
            override fun offer(
                level: ServerLevel,
                direction: Direction,
                variant: EnergyVariant,
                amount: Long,
            ): Long = offerEnergy(targets[level, direction], amount)

            override fun invalidate() = targets.invalidate()
        }
    }
}

private fun offerItem(
    target: IItemHandler?,
    variant: ItemVariant,
    amount: Long,
): Long {
    if (target == null || amount <= 0L) return 0L
    val offered =
        min(
            amount,
            variant.template.maxStackSize
                .coerceAtLeast(1)
                .toLong(),
        ).toInt()
    val remainder = ItemHandlerHelper.insertItemStacked(target, variant.template.copyWithCount(offered), false)
    return (offered - remainder.count.coerceIn(0, offered)).toLong()
}

private fun offerFluid(
    target: IFluidHandler?,
    variant: FluidVariant,
    amount: Long,
): Long {
    if (target == null || amount <= 0L) return 0L
    val offered = amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return target
        .fill(variant.template.copyWithAmount(offered), IFluidHandler.FluidAction.EXECUTE)
        .coerceIn(0, offered)
        .toLong()
}

private fun offerEnergy(
    target: IEnergyStorage?,
    amount: Long,
): Long {
    if (target?.canReceive() != true || amount <= 0L) return 0L
    val offered = amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return target.receiveEnergy(offered, false).coerceIn(0, offered).toLong()
}
