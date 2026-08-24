package rhx.lazy.core.configurator

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.column
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.acceptQuickMove
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.elements.withTooltips
import com.lowdragmc.lowdraglib2.gui.ui.inventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import net.minecraft.network.chat.Component
import rhx.lazy.core.lazyId
import rhx.lazy.core.ui.largeItemSlot

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
                                largeItemSlot(model.inventory, slot, spec = {
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
}
