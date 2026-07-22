package rhx.compose.block.entity

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

internal data class BufferItemSnapshot(
    val template: ItemStack,
    val count: Int,
)

internal data class BufferSnapshot(
    val items: List<BufferItemSnapshot>,
    val fluids: List<FluidStack>,
) {
    fun hasContents(): Boolean =
        items.any { item -> !item.template.isEmpty && item.count > 0 } ||
            fluids.any { fluid -> !fluid.isEmpty }

    fun write(buffer: RegistryFriendlyByteBuf) {
        items.forEach { item ->
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, item.template)
            buffer.writeVarInt(item.count)
        }
        fluids.forEach { fluid -> FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, fluid) }
    }

    companion object {
        fun read(buffer: RegistryFriendlyByteBuf): BufferSnapshot {
            val items =
                List(BufferBlockEntity.ITEM_SLOT_COUNT) {
                    val template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)
                    val count = buffer.readVarInt().coerceIn(0, BufferBlockEntity.ITEM_SLOT_CAPACITY)
                    if (template.isEmpty || count == 0) {
                        BufferItemSnapshot(ItemStack.EMPTY, 0)
                    } else {
                        BufferItemSnapshot(template.copyWithCount(1), count)
                    }
                }
            val fluids =
                List(BufferBlockEntity.FLUID_TANK_COUNT) {
                    val fluid = FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer)
                    if (fluid.isEmpty) {
                        FluidStack.EMPTY
                    } else {
                        fluid.copyWithAmount(fluid.amount.coerceIn(1, BufferBlockEntity.FLUID_TANK_CAPACITY))
                    }
                }
            return BufferSnapshot(items, fluids)
        }

        fun empty(): BufferSnapshot =
            BufferSnapshot(
                List(BufferBlockEntity.ITEM_SLOT_COUNT) { BufferItemSnapshot(ItemStack.EMPTY, 0) },
                List(BufferBlockEntity.FLUID_TANK_COUNT) { FluidStack.EMPTY },
            )
    }
}
