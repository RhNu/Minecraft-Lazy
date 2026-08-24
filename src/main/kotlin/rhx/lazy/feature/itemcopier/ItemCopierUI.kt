package rhx.lazy.feature.itemcopier

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.asXeiPhantom
import com.lowdragmc.lowdraglib2.gui.ui.elements.asXeiRecipeIngredient
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.itemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.inventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.io.IoPanelModel
import rhx.lazy.core.io.IoPanelUI
import rhx.lazy.core.lazyId

internal object ItemCopierUI {
    private val stylesheet = lazyId("lss/item_copier.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        val model = ItemCopierUiModel(holder)
        lateinit var templateSlot: ItemSlot
        lateinit var gearButton: Button
        lateinit var installIoPanel: (UIElement) -> Unit

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
                    templateSlot =
                        itemSlot(
                            {
                                cls = { +"lazy-item-copier__template" }
                                style = {
                                    tooltips(
                                        Component.translatable("gui.lazy.item_copier.template.empty"),
                                    )
                                }
                            },
                        ) {
                            asXeiPhantom()
                            asXeiRecipeIngredient(IngredientIO.OUTPUT)
                        }.element.apply {
                            bind(
                                DataBindingBuilder
                                    .itemStack(model::template, model::setTemplate)
                                    .initialValue(ItemStack.EMPTY)
                                    .build(),
                            )
                            addServerEventListener("mouseDown") { event ->
                                if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                    model.markCarriedStack()
                                }
                            }
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
                                onServerClick = { event ->
                                    if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                        model.cycleGear()
                                    }
                                }
                            },
                        ).element

                    installIoPanel = IoPanelUI.addIoControl(this, model)
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

        templateSlot.registerValueListener { template ->
            templateSlot.style { style ->
                style.tooltips(
                    if (template.isEmpty) {
                        Component.translatable("gui.lazy.item_copier.template.empty")
                    } else {
                        Component.translatable(
                            "gui.lazy.item_copier.template.selected",
                            template.hoverName,
                        )
                    },
                )
            }
        }

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
        installIoPanel(root)

        return ModularUI(
            UI.of(
                root,
                StylesheetManager.MC,
                stylesheet,
                IoPanelUI.stylesheet,
            ),
            holder.player,
        )
    }

    private class ItemCopierUiModel(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) : IoPanelModel {
        override val player = holder.player

        override val editor
            get() = blockEntity?.ioController

        private val blockEntity: ItemCopierBlockEntity?
            get() =
                holder.player.level().blockEntityOrNull(
                    holder.pos,
                    ItemCopierRegistries.blockEntity.get(),
                )

        fun template(): ItemStack = blockEntity?.getTemplate() ?: ItemStack.EMPTY

        fun setTemplate(stack: ItemStack) {
            if (!isValid()) return
            blockEntity?.setTemplate(stack)
        }

        fun gear(): ItemCopierGear = blockEntity?.getGear() ?: ItemCopierGear.DEFAULT

        fun markCarriedStack() {
            setTemplate(holder.player.containerMenu.carried)
        }

        fun cycleGear() {
            val entity = blockEntity ?: return
            if (!isValid()) return
            entity.cycleGear()
        }

        override fun isValid(): Boolean {
            val block = holder.blockState.block as? ItemCopierBlock ?: return false
            return block.stillValid(holder)
        }
    }

    private const val LEFT_MOUSE_BUTTON = 0
}
