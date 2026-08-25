package rhx.lazy.core.ui

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlotElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlotSpec
import com.lowdragmc.lowdraglib2.gui.ui.elements.dsl
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.ui.client.LargeItemSlotRenderer

/**
 * LDLib2 item slot whose icon is rendered from a one-item template and whose real quantity gets a
 * compact, width-constrained overlay.
 *
 * The visual template and count are synchronized separately from the vanilla menu slot. This is
 * necessary because vanilla's item-stack network validation rejects stacks above an item's normal
 * limit. Interaction remains LDLib2's normal container-slot interaction and is therefore compatible
 * with click, split, drag and quick-move.
 */
internal class LargeItemSlot(
    slot: Slot,
    private val countFormatter: LargeItemCountFormatter = compactItemCountFormatter,
) : ItemSlot(slot) {
    private var displayStack = normalizeDisplayStack(slot.item)
    private var displayCount = if (slot.item.isEmpty) 0L else slot.item.count.toLong()

    /** Preserve the element identity used by LDLib2's `item-slot` stylesheet selector. */
    override fun name(): String = ITEM_SLOT_ELEMENT_NAME

    override fun getValue(): ItemStack = displayStack

    override fun setValue(
        value: ItemStack?,
        notify: Boolean,
    ): LargeItemSlot {
        val normalized = normalizeDisplayStack(value)
        if (ItemStack.matches(displayStack, normalized)) return this
        displayStack = normalized
        if (notify) notifyListeners()
        return this
    }

    internal fun setDisplayCount(count: Long) {
        displayCount = count.coerceAtLeast(0L)
    }

    override fun drawItemStack(
        context: GUIContext,
        itemStack: ItemStack,
    ) {
        LargeItemSlotRenderer.draw(
            context,
            itemStack,
            displayCount,
            countFormatter,
        )
    }

    private companion object {
        private const val ITEM_SLOT_ELEMENT_NAME = "item-slot"

        private fun normalizeDisplayStack(stack: ItemStack?): ItemStack =
            if (stack == null || stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1)
    }
}

/** Adds an interactive high-capacity handler slot to an LDLib2 Kotlin UI tree. */
internal fun UIContainer<*, *>.largeItemSlot(
    handler: IItemHandlerModifiable,
    index: Int,
    countProvider: (() -> Long)? = null,
    countFormatter: LargeItemCountFormatter = compactItemCountFormatter,
    spec: (ItemSlotSpec<LargeItemSlot>.() -> Unit)? = null,
    init: ItemSlotElement<LargeItemSlot>.() -> Unit = {},
): ItemSlotElement<LargeItemSlot> {
    val stackProvider = {
        handler
            .getStackInSlot(index)
            .let { stack -> if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1) }
    }
    val resolvedCountProvider = countProvider ?: { handler.getStackInSlot(index).count.toLong() }
    val builder =
        LargeItemSlot(
            LargeItemHandlerSlot(handler, index),
            countFormatter,
        ).dsl(spec, init)
    builder.element.addClass(ITEM_SLOT_BACKGROUND_CLASS)
    val displayStack = BindableValue(ItemStack.EMPTY)
    displayStack.setDisplay(false)
    displayStack.registerValueListener { stack -> builder.element.setValue(stack, false) }
    displayStack.bind(
        DataBindingBuilder
            .itemStackS2C(stackProvider)
            .initialValue(ItemStack.EMPTY)
            .build(),
    )
    builder.element.addChild(displayStack)

    val displayCount = BindableValue(0L)
    displayCount.setDisplay(false)
    displayCount.registerValueListener(builder.element::setDisplayCount)
    displayCount.bind(
        DataBindingBuilder
            .longValS2C(resolvedCountProvider)
            .initialValue(0L)
            .build(),
    )
    builder.element.addChild(displayCount)

    addChild(builder)
    return builder
}

private const val ITEM_SLOT_BACKGROUND_CLASS = "item-slot_bg"
