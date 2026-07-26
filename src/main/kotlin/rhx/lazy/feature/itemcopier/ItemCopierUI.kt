package rhx.lazy.feature.itemcopier

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.inventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.lazyId

internal object ItemCopierUI {
    private val stylesheet = lazyId("lss/item_copier.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        val model = ItemCopierUiModel(holder)
        val templateTexture = ItemStackTexture(ItemStack.EMPTY)
        lateinit var templateButton: Button
        lateinit var gearButton: Button

        val root =
            element(
                {
                    cls = {
                        +"panel_bg"
                        +"lazy-item-copier"
                    }
                },
            ) {
                label(
                    {
                        text = Component.translatable("block.lazy.item_copier")
                        cls = { +"lazy-item-copier__title" }
                    },
                )

                row(
                    {
                        cls = { +"lazy-item-copier__controls" }
                    },
                ) {
                    templateButton =
                        button(
                            {
                                noText()
                                cls = { +"lazy-item-copier__template" }
                                style = {
                                    tooltips(
                                        Component.translatable("gui.lazy.item_copier.template.empty"),
                                        Component.translatable("gui.lazy.item_copier.template.description"),
                                    )
                                }
                                onServerClick = { event ->
                                    if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                        model.markCarriedStack()
                                    }
                                }
                            },
                        ).element.apply {
                            buttonStyle { style ->
                                style.baseTexture(ItemSlot.ITEM_SLOT_TEXTURE)
                                style.hoverTexture(ItemSlot.ITEM_SLOT_TEXTURE)
                                style.pressedTexture(ItemSlot.ITEM_SLOT_TEXTURE)
                            }
                            addPreIcon(templateTexture)
                        }

                    gearButton =
                        button(
                            {
                                text =
                                    Component.translatable(
                                        "gui.lazy.item_copier.interval",
                                        ItemCopierGear.DEFAULT.intervalTicks,
                                    )
                                cls = { +"lazy-item-copier__gear" }
                                style = {
                                    tooltips(
                                        Component.translatable(
                                            "gui.lazy.item_copier.interval.description",
                                        ),
                                    )
                                }
                                onServerClick = { event ->
                                    if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                        model.cycleGear()
                                    }
                                }
                            },
                        ).element
                }

                label(
                    {
                        text = Component.translatable("container.inventory")
                        cls = { +"lazy-item-copier__inventory-title" }
                    },
                )

                inventorySlots(
                    {
                        cls = { +"lazy-item-copier__inventory" }
                    },
                )
            }

        val displayedTemplate = BindableValue(ItemStack.EMPTY)
        displayedTemplate.setDisplay(false)
        displayedTemplate.registerValueListener { template ->
            templateTexture.setItems(template)
            templateButton.style { style ->
                style.tooltips(
                    if (template.isEmpty) {
                        Component.translatable("gui.lazy.item_copier.template.empty")
                    } else {
                        Component.translatable(
                            "gui.lazy.item_copier.template.selected",
                            template.hoverName,
                        )
                    },
                    Component.translatable("gui.lazy.item_copier.template.description"),
                )
            }
        }
        displayedTemplate.bind(
            DataBindingBuilder
                .itemStackS2C(model::template)
                .build(),
        )
        root.addChild(displayedTemplate)

        val displayedGear = BindableValue(ItemCopierGear.DEFAULT)
        displayedGear.setDisplay(false)
        displayedGear.registerValueListener { gear ->
            gearButton.setText(
                Component.translatable(
                    "gui.lazy.item_copier.interval",
                    gear.intervalTicks,
                ),
            )
        }
        displayedGear.bind(
            DataBindingBuilder
                .enumValS2C(ItemCopierGear::class.java, model::gear)
                .initialValue(ItemCopierGear.DEFAULT)
                .build(),
        )
        root.addChild(displayedGear)

        return ModularUI(
            UI.of(
                root,
                StylesheetManager.MC,
                stylesheet,
            ),
            holder.player,
        )
    }

    private class ItemCopierUiModel(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) {
        private val blockEntity: ItemCopierBlockEntity?
            get() =
                holder.player.level().blockEntityOrNull(
                    holder.pos,
                    ItemCopierRegistries.blockEntity.get(),
                )

        fun template(): ItemStack = blockEntity?.getTemplate() ?: ItemStack.EMPTY

        fun gear(): ItemCopierGear = blockEntity?.getGear() ?: ItemCopierGear.DEFAULT

        fun markCarriedStack() {
            val entity = blockEntity ?: return
            if (!isValid()) return
            entity.setTemplate(holder.player.containerMenu.carried)
        }

        fun cycleGear() {
            val entity = blockEntity ?: return
            if (!isValid()) return
            entity.cycleGear()
        }

        fun isValid(): Boolean {
            val block = holder.blockState.block as? ItemCopierBlock ?: return false
            return block.stillValid(holder)
        }
    }

    private const val LEFT_MOUSE_BUTTON = 0
}
