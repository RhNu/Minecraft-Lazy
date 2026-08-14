package rhx.lazy.feature.simulation

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.neoforged.neoforge.fluids.FluidStack

internal class LongFluidStack(
    stack: FluidStack,
    val amount: Long,
) {
    private val storedTemplate = stack.copyWithAmount(1)

    init {
        require(!stack.isEmpty && amount > 0L)
    }

    val template: FluidStack
        get() = storedTemplate.copy()

    fun matches(stack: FluidStack): Boolean = !stack.isEmpty && FluidStack.isSameFluidSameComponents(storedTemplate, stack)

    fun withAmount(value: Long) = LongFluidStack(storedTemplate, value)

    fun plus(value: Long): LongFluidStack = withAmount(if (value > Long.MAX_VALUE - amount) Long.MAX_VALUE else amount + value)

    fun save(registries: HolderLookup.Provider): CompoundTag =
        CompoundTag().apply {
            put(
                "stack",
                FluidStack.CODEC
                    .encodeStart(
                        registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                        storedTemplate,
                    ).result()
                    .orElse(CompoundTag()),
            )
            putLong("amount", amount)
        }

    companion object {
        fun parse(
            registries: HolderLookup.Provider,
            tag: CompoundTag,
        ): LongFluidStack? {
            val stack =
                FluidStack.CODEC
                    .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("stack"))
                    .result()
                    .orElse(FluidStack.EMPTY)
            val amount = tag.getLong("amount")
            return if (stack.isEmpty || amount <= 0) null else LongFluidStack(stack, amount)
        }
    }
}
