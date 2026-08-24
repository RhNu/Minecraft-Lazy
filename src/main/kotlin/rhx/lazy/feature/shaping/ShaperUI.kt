package rhx.lazy.feature.shaping

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.slot.LocalSlot
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.column
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.acceptQuickMove
import com.lowdragmc.lowdraglib2.gui.ui.elements.asXeiPhantom
import com.lowdragmc.lowdraglib2.gui.ui.elements.asXeiRecipeIngredient
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.itemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.elements.withTooltips
import com.lowdragmc.lowdraglib2.gui.ui.inventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.io.IoPanelModel
import rhx.lazy.core.io.IoPanelUI
import rhx.lazy.core.lazyId
import rhx.lazy.core.material.MaterialIndexes

internal object ShaperUI {
    private val stylesheet = lazyId("lss/shaper.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        val model = ShaperUiModel(holder)
        lateinit var sampleSlot: UIElement
        lateinit var warning: UIElement
        lateinit var installIoPanel: (UIElement) -> Unit

        val root =
            element(
                {
                    cls = {
                        +"panel_bg"
                        +"lazy-shaper"
                    }
                },
            ) {
                label(
                    {
                        text = Component.translatable("block.lazy.shaper")
                        cls = { +"lazy-shaper__title" }
                    },
                )

                row(
                    {
                        cls = { +"lazy-shaper__settings" }
                    },
                ) {
                    sampleSlot =
                        itemSlot(
                            {
                                bind(SampleSlot(model::isValidSample))
                                cls = { +"lazy-shaper__slot" }
                            },
                        ) {
                            asXeiPhantom()
                            asXeiRecipeIngredient(IngredientIO.INPUT)
                        }.element.apply {
                            bind(
                                DataBindingBuilder
                                    .itemStack(model::sample, model::setSample)
                                    .initialValue(ItemStack.EMPTY)
                                    .build(),
                            )
                            addServerEventListener("mouseDown") { event ->
                                if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                    model.markCarriedStack()
                                }
                            }
                        }
                    warning =
                        button(
                            {
                                noText()
                                active = false
                                visible = false
                                cls = { +"lazy-shaper__warning" }
                                style = { tooltips(Component.translatable("gui.lazy.shaper.sample.unknown")) }
                            },
                        ).element.apply {
                            addPreIcon(ItemStackTexture(ItemStack(Items.BARRIER)))
                        }
                    installIoPanel = IoPanelUI.addIoControl(this, model)
                }

                row(
                    {
                        cls = { +"lazy-shaper__storage" }
                    },
                ) {
                    laneGrid("gui.lazy.shaper.input", "lazy-shaper__input", model.inputHandler, IngredientIO.INPUT)
                    laneGrid("gui.lazy.shaper.output", "lazy-shaper__output", model.outputHandler, IngredientIO.OUTPUT)
                }

                label(
                    {
                        text = Component.translatable("container.inventory")
                        cls = { +"lazy-shaper__inventory-title" }
                    },
                )
                inventorySlots(
                    {
                        cls = { +"lazy-shaper__inventory" }
                    },
                )
            }

        val tooltip = BindableValue<Component>(Component.translatable("gui.lazy.shaper.sample.empty"))
        tooltip.setDisplay(false)
        tooltip.registerValueListener { component ->
            sampleSlot.style { style -> style.tooltips(component) }
        }
        tooltip.bind(DataBindingBuilder.componentS2C(model::conversionTooltip).build())
        root.addChild(tooltip)

        val invalidSample = BindableValue(false)
        invalidSample.setDisplay(false)
        invalidSample.registerValueListener(warning::setVisible)
        invalidSample.bind(DataBindingBuilder.boolS2C(model::hasInvalidSample).initialValue(false).build())
        root.addChild(invalidSample)
        installIoPanel(root)

        return ModularUI(
            UI.of(root, StylesheetManager.MC, stylesheet, IoPanelUI.stylesheet),
            holder.player,
        )
    }

    private fun com.lowdragmc.lowdraglib2.gui.ui.UIContainer<*, *>.laneGrid(
        titleKey: String,
        cssClass: String,
        handler: IItemHandlerModifiable,
        ingredientIo: IngredientIO,
    ) {
        column(
            {
                cls = {
                    +"lazy-shaper__lane-group"
                    +cssClass
                }
            },
        ) {
            label(
                {
                    text = Component.translatable(titleKey)
                    cls = { +"lazy-shaper__lane-title" }
                },
            )
            repeat(2) { rowIndex ->
                row(
                    {
                        cls = { +"lazy-shaper__lane-row" }
                    },
                ) {
                    repeat(4) { columnIndex ->
                        val slot = rowIndex * 4 + columnIndex
                        itemSlot(
                            {
                                bind(handler, slot)
                                cls = { +"lazy-shaper__slot" }
                            },
                        ) {
                            withTooltips()
                            if (ingredientIo == IngredientIO.INPUT) acceptQuickMove()
                            asXeiRecipeIngredient(ingredientIo)
                        }
                    }
                }
            }
        }
    }

    private class ShaperUiModel(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) : IoPanelModel {
        override val player = holder.player

        override val editor
            get() = blockEntity?.ioController

        private val blockEntity: ShaperBlockEntity?
            get() = holder.player.level().blockEntityOrNull(holder.pos, ShaperRegistries.blockEntity.get())

        val inputHandler: IItemHandlerModifiable
            get() = blockEntity?.inputHandler ?: EmptyShaperHandler

        val outputHandler: IItemHandlerModifiable
            get() = blockEntity?.outputHandler ?: EmptyShaperHandler

        fun sample(): ItemStack = blockEntity?.sampleStack() ?: ItemStack.EMPTY

        fun setSample(stack: ItemStack) {
            if (!isValid()) return
            blockEntity?.setSample(stack)
        }

        fun markCarriedStack() {
            setSample(holder.player.containerMenu.carried)
        }

        fun isValidSample(stack: ItemStack): Boolean = !stack.isEmpty && MaterialIndexes.current().formOf(stack.item) != null

        fun conversionTooltip(): Component = blockEntity?.conversionTooltip() ?: Component.translatable("gui.lazy.shaper.sample.empty")

        fun hasInvalidSample(): Boolean = blockEntity?.hasInvalidSample() == true

        override fun isValid(): Boolean = (holder.blockState.block as? ShaperBlock)?.stillValid(holder) == true
    }

    private object EmptyShaperHandler : EmptyHandler(ShaperBlockEntity.LANES, ShaperBlockEntity.LANE_CAPACITY)

    private open class EmptyHandler(
        private val size: Int,
        private val limit: Int,
    ) : IItemHandlerModifiable {
        override fun getSlots(): Int = size

        override fun getStackInSlot(slot: Int): ItemStack = ItemStack.EMPTY

        override fun insertItem(
            slot: Int,
            stack: ItemStack,
            simulate: Boolean,
        ): ItemStack = stack

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean,
        ): ItemStack = ItemStack.EMPTY

        override fun getSlotLimit(slot: Int): Int = limit

        override fun isItemValid(
            slot: Int,
            stack: ItemStack,
        ): Boolean = false

        override fun setStackInSlot(
            slot: Int,
            stack: ItemStack,
        ) = Unit
    }

    private class SampleSlot(
        private val validator: (ItemStack) -> Boolean,
    ) : LocalSlot() {
        override fun mayPlace(stack: ItemStack): Boolean = validator(stack)

        override fun getMaxStackSize(): Int = 1
    }

    private const val LEFT_MOUSE_BUTTON = 0
}
