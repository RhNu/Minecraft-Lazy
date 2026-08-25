package rhx.lazy.integration.mysticalagriculture

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.column
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.acceptQuickMove
import com.lowdragmc.lowdraglib2.gui.ui.elements.asXeiRecipeIngredient
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.itemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.elements.withTooltips
import com.lowdragmc.lowdraglib2.gui.ui.inventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO
import dev.vfyjxf.taffy.style.AlignContent
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.TaffyPosition
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.io.IoPanelModel
import rhx.lazy.core.io.IoPanelUI
import rhx.lazy.core.lazyId
import rhx.lazy.core.ui.CompactLongFormatter

internal object EssenceConverterUI {
    private val stylesheet = lazyId("lss/essence_converter.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        val model = EssenceConverterUiModel(holder)
        val targetButtons = mutableMapOf<EssenceTier, UIElement>()
        lateinit var amountDisplay: UIElement
        lateinit var remainderDisplay: UIElement
        lateinit var inputSlot: UIElement
        lateinit var lockedInputSlot: UIElement
        lateinit var clearButton: UIElement
        lateinit var confirmationLayer: UIElement
        lateinit var installIoPanel: (UIElement) -> Unit

        val root =
            element(
                {
                    cls = {
                        +"panel_bg"
                        +"lazy-essence-converter"
                    }
                },
            ) {
                row(
                    {
                        cls = { +"lazy-essence-converter__status" }
                    },
                ) {
                    amountDisplay =
                        label(
                            {
                                cls = {
                                    +"lazy-essence-converter__counter"
                                    +"lazy-essence-converter__amount"
                                }
                            },
                        ) {
                            bind(componentBinding(model::amount))
                        }.element
                    remainderDisplay =
                        label(
                            {
                                cls = {
                                    +"lazy-essence-converter__counter"
                                    +"lazy-essence-converter__remainder"
                                }
                            },
                        ) {
                            bind(componentBinding(model::remainder))
                        }.element
                }

                row(
                    {
                        cls = { +"lazy-essence-converter__targets" }
                    },
                ) {
                    EssenceTier.entries.forEach { tier ->
                        val available = tier.isAvailable()
                        targetButtons[tier] =
                            button(
                                {
                                    active = available
                                    noText()
                                    cls = { +"lazy-essence-converter__icon-button" }
                                    style = {
                                        tooltips(
                                            if (available) {
                                                tier.createStack().hoverName
                                            } else {
                                                Component.translatable("gui.lazy.essence_converter.requires_agradditions")
                                            },
                                        )
                                    }
                                    onServerClick = { event ->
                                        if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                            model.selectTarget(tier)
                                        }
                                    }
                                },
                            ).element.apply {
                                addPreIcon(
                                    ItemStackTexture(
                                        if (available) tier.createStack() else ItemStack(Items.BARRIER),
                                    ),
                                )
                            }
                    }
                }

                row(
                    {
                        cls = { +"lazy-essence-converter__controls" }
                    },
                ) {
                    inputSlot =
                        itemSlot(
                            {
                                bind(model.inputHandler, 0)
                                cls = { +"lazy-essence-converter__slot" }
                            },
                        ) {
                            withTooltips()
                            acceptQuickMove()
                            asXeiRecipeIngredient(IngredientIO.INPUT)
                        }.element.apply { setDisplay(false) }
                    lockedInputSlot =
                        button(
                            {
                                active = false
                                noText()
                                cls = {
                                    +"lazy-essence-converter__icon-button"
                                    +"lazy-essence-converter__locked-input"
                                }
                                style = {
                                    tooltips(Component.translatable("gui.lazy.essence_converter.select_target"))
                                }
                            },
                        ).element.apply {
                            addPreIcon(ItemStackTexture(ItemStack(Items.BARRIER)))
                        }
                    row(
                        {
                            cls = { +"lazy-essence-converter__actions" }
                        },
                    ) {
                        installIoPanel = IoPanelUI.addIoControl(this, model)
                        clearButton =
                            button(
                                {
                                    noText()
                                    active = false
                                    cls = { +"lazy-io__trigger" }
                                    style = { tooltips(Component.translatable("gui.lazy.essence_converter.clear")) }
                                    onClick = { confirmationLayer.setVisible(true) }
                                },
                            ).element.apply {
                                addPreIcon(ItemStackTexture(ItemStack(Items.BARRIER)))
                            }
                    }
                }

                inventorySlots(
                    {
                        cls = { +"lazy-essence-converter__inventory" }
                    },
                )

                confirmationLayer =
                    element(
                        {
                            visible = false
                            cls = { +"lazy-essence-converter__confirmation-layer" }
                            layout = {
                                position(TaffyPosition.ABSOLUTE)
                                width(100.pct)
                                height(100.pct)
                                pos {
                                    left(0)
                                    top(0)
                                }
                                justifyContent(AlignContent.CENTER)
                                alignItems(AlignItems.CENTER)
                            }
                        },
                    ) {
                        column(
                            {
                                cls = {
                                    +"panel_bg"
                                    +"lazy-essence-converter__confirmation"
                                }
                            },
                        ) {
                            label({ text = Component.translatable("gui.lazy.essence_converter.clear.confirm") })
                            row {
                                button(
                                    {
                                        text = Component.translatable("gui.lazy.essence_converter.clear.accept")
                                        onClick = { confirmationLayer.setVisible(false) }
                                        onServerClick = {
                                            if (model.isValid()) model.clearContents()
                                        }
                                    },
                                )
                                button(
                                    {
                                        text = Component.translatable("gui.lazy.essence_converter.clear.cancel")
                                        onClick = { confirmationLayer.setVisible(false) }
                                    },
                                )
                            }
                        }
                    }.element
            }

        targetButtons.forEach { (tier, button) ->
            bindSelected(root, button) { model.targetTier() == tier }
        }
        bindTooltip(root, amountDisplay, model::amountTooltip)
        bindTooltip(root, remainderDisplay, model::remainderTooltip)
        bindInputAvailability(root, inputSlot, lockedInputSlot, model::hasTarget)
        val hasContents = BindableValue(false)
        hasContents.setDisplay(false)
        hasContents.registerValueListener(clearButton::setActive)
        hasContents.bind(booleanBinding(model::hasContents))
        root.addChild(hasContents)
        installIoPanel(root)

        return ModularUI(
            UI.of(root, StylesheetManager.MC, stylesheet, IoPanelUI.stylesheet),
            holder.player,
        )
    }

    private fun bindSelected(
        root: UIElement,
        button: UIElement,
        selected: () -> Boolean,
    ) {
        val value = BindableValue(false)
        value.setDisplay(false)
        value.registerValueListener { isSelected ->
            if (isSelected) button.addClass(SELECTED_BUTTON_CLASS) else button.removeClass(SELECTED_BUTTON_CLASS)
        }
        value.bind(booleanBinding(selected))
        root.addChild(value)
    }

    private fun bindTooltip(
        root: UIElement,
        element: UIElement,
        tooltip: () -> Component,
    ) {
        val value = BindableValue<Component>(Component.empty())
        value.setDisplay(false)
        value.registerValueListener { component ->
            element.style { style -> style.tooltips(component) }
        }
        value.bind(componentBinding(tooltip))
        root.addChild(value)
    }

    private fun bindInputAvailability(
        root: UIElement,
        inputSlot: UIElement,
        lockedInputSlot: UIElement,
        hasTarget: () -> Boolean,
    ) {
        val value = BindableValue(false)
        value.setDisplay(false)
        value.registerValueListener { available ->
            inputSlot.setDisplay(available)
            lockedInputSlot.setDisplay(!available)
        }
        value.bind(booleanBinding(hasTarget))
        root.addChild(value)
    }

    private class EssenceConverterUiModel(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) : IoPanelModel {
        override val player = holder.player

        override val editor
            get() = blockEntity?.ioController

        private val blockEntity: EssenceConverterBlockEntity?
            get() =
                holder.player.level().blockEntityOrNull(
                    holder.pos,
                    EssenceConverterRegistries.blockEntity.get(),
                )

        val inputHandler: IItemHandlerModifiable
            get() = blockEntity?.inputHandler ?: EmptyItemHandler

        fun targetTier(): EssenceTier? = blockEntity?.targetTier

        fun hasTarget(): Boolean = targetTier() != null

        fun hasContents(): Boolean = blockEntity?.hasContents() == true

        fun amount(): Component = Component.literal(CompactLongFormatter.format(blockEntity?.outputCount ?: 0L))

        fun remainder(): Component = Component.literal((blockEntity?.remainderUnits ?: 0).toString())

        fun amountTooltip(): Component {
            val entity = blockEntity ?: return Component.translatable("gui.lazy.essence_converter.unavailable")
            return Component.translatable(
                "gui.lazy.essence_converter.amount.tooltip",
                entity.outputCount,
                entity.capacity,
            )
        }

        fun remainderTooltip(): Component {
            val entity = blockEntity ?: return Component.translatable("gui.lazy.essence_converter.unavailable")
            val limit: Any = entity.targetTier?.inferiumValue ?: Component.literal("-")
            return Component.translatable(
                "gui.lazy.essence_converter.remainder.tooltip",
                entity.remainderUnits,
                limit,
            )
        }

        fun selectTarget(tier: EssenceTier) {
            val entity = blockEntity ?: return
            if (!entity.selectTarget(tier)) {
                holder.player.displayActionBar("message.lazy.essence_converter.target_locked")
            }
        }

        fun clearContents() {
            blockEntity?.clearContents()
        }

        override fun isValid(): Boolean {
            val block = holder.blockState.block as? EssenceConverterBlock ?: return false
            return block.stillValid(holder)
        }
    }

    private fun componentBinding(value: () -> Component) =
        DataBindingBuilder
            .componentS2C { value() }
            .build()

    private fun booleanBinding(value: () -> Boolean) =
        DataBindingBuilder
            .boolS2C { value() }
            .initialValue(false)
            .build()

    private object EmptyItemHandler : IItemHandlerModifiable {
        override fun getSlots(): Int = 1

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

        override fun getSlotLimit(slot: Int): Int = 0

        override fun isItemValid(
            slot: Int,
            stack: ItemStack,
        ): Boolean = false

        override fun setStackInSlot(
            slot: Int,
            stack: ItemStack,
        ) = Unit
    }

    private const val LEFT_MOUSE_BUTTON = 0
    private const val SELECTED_BUTTON_CLASS = "lazy-essence-converter__icon-button--selected"
}
