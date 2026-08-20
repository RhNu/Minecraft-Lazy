package rhx.lazy.core.configurator

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandlerModifiable
import kotlin.math.min

internal class ModularConfiguratorInventory(
    private val configurator: ItemStack,
    private val acceptsMaterial: (ItemStack) -> Boolean = ModularConfiguratorModules::acceptsMaterial,
) : IItemHandlerModifiable {
    override fun getSlots(): Int = ModularConfiguratorData.SLOT_COUNT

    override fun getStackInSlot(slot: Int): ItemStack = data().stack(checkedSlot(slot))

    override fun insertItem(
        slot: Int,
        stack: ItemStack,
        simulate: Boolean,
    ): ItemStack {
        checkedSlot(slot)
        if (stack.isEmpty || !acceptsMaterial(stack)) return stack
        val stored = data().stack(slot)
        if (!stored.isEmpty && !ItemStack.isSameItemSameComponents(stored, stack)) return stack
        val accepted = min(stack.count, ModularConfiguratorData.SLOT_LIMIT - stored.count)
        if (accepted <= 0) return stack
        if (!simulate) {
            val updated =
                if (stored.isEmpty) {
                    stack.copyWithCount(accepted)
                } else {
                    stored.copyWithCount(stored.count + accepted)
                }
            setData(data().withStack(slot, updated))
        }
        return if (accepted == stack.count) ItemStack.EMPTY else stack.copyWithCount(stack.count - accepted)
    }

    override fun extractItem(
        slot: Int,
        amount: Int,
        simulate: Boolean,
    ): ItemStack {
        val stored = data().stack(checkedSlot(slot))
        if (stored.isEmpty || amount <= 0) return ItemStack.EMPTY
        val extracted = min(amount, stored.count)
        val result = stored.copyWithCount(extracted)
        if (!simulate) {
            val remaining = stored.count - extracted
            setData(data().withStack(slot, if (remaining == 0) ItemStack.EMPTY else stored.copyWithCount(remaining)))
        }
        return result
    }

    override fun getSlotLimit(slot: Int): Int {
        checkedSlot(slot)
        return ModularConfiguratorData.SLOT_LIMIT
    }

    override fun isItemValid(
        slot: Int,
        stack: ItemStack,
    ): Boolean {
        checkedSlot(slot)
        return !stack.isEmpty && acceptsMaterial(stack)
    }

    override fun setStackInSlot(
        slot: Int,
        stack: ItemStack,
    ) {
        checkedSlot(slot)
        if (stack.isEmpty) {
            setData(data().withStack(slot, ItemStack.EMPTY))
        } else if (acceptsMaterial(stack)) {
            setData(data().withStack(slot, stack.copyWithCount(stack.count.coerceAtMost(ModularConfiguratorData.SLOT_LIMIT))))
        }
    }

    private fun data(): ModularConfiguratorData = ModularConfiguratorDataAccess.get(configurator)

    private fun setData(data: ModularConfiguratorData) {
        ModularConfiguratorDataAccess.set(configurator, data)
    }

    private fun checkedSlot(slot: Int): Int {
        require(slot in 0 until ModularConfiguratorData.SLOT_COUNT) { "Invalid modular configurator slot $slot" }
        return slot
    }
}

internal object ModularConfiguratorDataAccess {
    fun get(stack: ItemStack): ModularConfiguratorData =
        stack.get(ModularConfiguratorRegistries.dataComponent.get()) ?: ModularConfiguratorData.EMPTY

    fun set(
        stack: ItemStack,
        data: ModularConfiguratorData,
    ) {
        require(ModularConfiguratorRegistries.isConfigurator(stack)) {
            "Cannot store modular configurator data on ${stack.item}"
        }
        if (data == ModularConfiguratorData.EMPTY) {
            stack.remove(ModularConfiguratorRegistries.dataComponent.get())
        } else {
            stack.set(ModularConfiguratorRegistries.dataComponent.get(), data)
        }
    }

    fun modulePayload(
        stack: ItemStack,
        id: net.minecraft.resources.ResourceLocation,
    ) = get(stack).modulePayload(id)

    fun setModulePayload(
        stack: ItemStack,
        id: net.minecraft.resources.ResourceLocation,
        payload: net.minecraft.nbt.CompoundTag,
    ) {
        set(stack, get(stack).withModulePayload(id, payload))
    }

    fun clearModules(stack: ItemStack): Boolean {
        val current = get(stack)
        if (!current.hasModulePayloads()) return false
        set(stack, current.clearModulePayloads())
        return true
    }
}
