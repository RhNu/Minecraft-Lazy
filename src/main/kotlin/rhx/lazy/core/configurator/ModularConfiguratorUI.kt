package rhx.lazy.core.configurator

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType
import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.column
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.acceptQuickMove
import com.lowdragmc.lowdraglib2.gui.ui.elements.itemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.elements.withTooltips
import com.lowdragmc.lowdraglib2.gui.ui.inventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.lazyId
import kotlin.math.min

internal object ModularConfiguratorUI {
    private val stylesheet = lazyId("lss/modular_configurator.lss")

    fun create(holder: HeldItemUIMenuType.HeldItemUIHolder): ModularUI {
        val model = Model(holder)
        val root =
            element({
                cls = {
                    +"panel_bg"
                    +"lazy-modular-configurator"
                }
            }) {
                label({
                    text = Component.translatable("item.lazy.modular_configurator")
                    cls = { +"lazy-modular-configurator__title" }
                })
                column({ cls = { +"lazy-modular-configurator__storage-group" } }) {
                    repeat(2) { rowIndex ->
                        row({ cls = { +"lazy-modular-configurator__storage" } }) {
                            repeat(9) { columnIndex ->
                                val slot = rowIndex * 9 + columnIndex
                                itemSlot({
                                    bind(HighCapacitySlot(model.inventory, slot))
                                    cls = { +"lazy-modular-configurator__slot" }
                                }) {
                                    withTooltips()
                                    acceptQuickMove()
                                }
                            }
                        }
                    }
                }
                label({
                    text = Component.translatable("container.inventory")
                    cls = { +"lazy-modular-configurator__inventory-title" }
                })
                inventorySlots({ cls = { +"lazy-modular-configurator__inventory" } })
            }
        return ModularUI(UI.of(root, StylesheetManager.MC, stylesheet), holder.player)
    }

    private class Model(
        private val holder: HeldItemUIMenuType.HeldItemUIHolder,
    ) {
        val inventory = ModularConfiguratorInventory(holder.itemStack)

        @Suppress("unused")
        fun isValid(): Boolean = ModularConfiguratorRegistries.isConfigurator(holder.itemStack)
    }

    /** Vanilla Slot clamps to an item's ordinary max stack size; configurator slots intentionally do not. */
    private class HighCapacitySlot(
        handler: IItemHandlerModifiable,
        index: Int,
    ) : ItemHandlerSlot(handler, index) {
        override fun getMaxStackSize(stack: ItemStack): Int = getMaxStackSize()

        /** Never put an overstacked cursor stack into the player's inventory or the world. */
        override fun remove(amount: Int): ItemStack = super.remove(min(amount, item.maxStackSize.coerceAtLeast(1)))
    }
}
