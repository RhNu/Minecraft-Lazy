package rhx.lazy.feature.replicator

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField
import com.lowdragmc.lowdraglib2.gui.ui.elements.asNumeric
import com.lowdragmc.lowdraglib2.gui.ui.elements.asXeiPhantom
import com.lowdragmc.lowdraglib2.gui.ui.elements.asXeiRecipeIngredient
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.fluidSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.itemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.elements.textField
import com.lowdragmc.lowdraglib2.gui.ui.inventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO
import dev.vfyjxf.taffy.style.TaffyDimension
import dev.vfyjxf.taffy.style.TaffyPosition
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.io.IoPanelModel
import rhx.lazy.core.io.IoPanelUI
import rhx.lazy.core.lazyId
import rhx.lazy.core.ui.CompactLongFormatter

internal object ReplicatorUI {
    private val stylesheet = lazyId("lss/replicator.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        val model = ReplicatorUiModel(holder)
        val amountEditor = AmountEditor(model)
        lateinit var itemTemplateSlot: ItemSlot
        lateinit var fluidTemplateSlot: FluidSlot
        lateinit var amountButton: Button
        lateinit var gearButton: Button
        lateinit var installIoPanel: (UIElement) -> Unit

        val root =
            element(
                {
                    cls = {
                        +"panel_bg"
                        +"lazy-replicator"
                    }
                },
            ) {
                label(
                    {
                        text = Component.translatable("block.lazy.replicator")
                        cls = { +"lazy-replicator__title" }
                    },
                )

                row(
                    {
                        cls = { +"lazy-replicator__controls" }
                    },
                ) {
                    element(
                        {
                            cls = { +"lazy-replicator__resource-slot" }
                        },
                    ) {
                        itemTemplateSlot =
                            itemSlot(
                                {
                                    cls = { +"lazy-replicator__resource-layer" }
                                    layout = {
                                        position(TaffyPosition.ABSOLUTE)
                                        pos {
                                            top(0)
                                            left(0)
                                        }
                                    }
                                },
                            ) {
                                asXeiPhantom()
                                asXeiRecipeIngredient(IngredientIO.OUTPUT)
                            }.element.apply {
                                bind(
                                    DataBindingBuilder
                                        .itemStack(model::itemTemplate, model::setItemTemplate)
                                        .initialValue(ItemStack.EMPTY)
                                        .build(),
                                )
                                installResourceInteraction(model, amountEditor)
                            }

                        fluidTemplateSlot =
                            fluidSlot(
                                {
                                    cls = { +"lazy-replicator__resource-layer" }
                                    layout = {
                                        position(TaffyPosition.ABSOLUTE)
                                        pos {
                                            top(0)
                                            left(0)
                                        }
                                    }
                                },
                            ) {
                                asXeiPhantom()
                                asXeiRecipeIngredient(IngredientIO.OUTPUT)
                            }.element.apply {
                                bind(
                                    DataBindingBuilder
                                        .fluidStack(model::fluidTemplate, model::setFluidTemplate)
                                        .initialValue(FluidStack.EMPTY)
                                        .build(),
                                )
                                installResourceInteraction(model, amountEditor)
                            }
                    }

                    amountButton =
                        button(
                            {
                                text = Component.literal("—")
                                cls = { +"lazy-replicator__amount" }
                                style = {
                                    tooltips(Component.translatable("gui.lazy.replicator.amount.edit"))
                                }
                                onClick = { event ->
                                    if (event.button == LEFT_MOUSE_BUTTON) {
                                        amountEditor.open()
                                    }
                                }
                            },
                        ).element

                    gearButton =
                        button(
                            {
                                text =
                                    Component.translatable(
                                        "gui.lazy.replicator.interval",
                                        ReplicatorGear.DEFAULT.intervalTicks,
                                    )
                                cls = { +"lazy-replicator__gear" }
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
                        cls = { +"lazy-replicator__inventory-title" }
                    },
                )

                inventorySlots(
                    {
                        cls = { +"lazy-replicator__inventory" }
                    },
                )
            }

        var displayedItem = ItemStack.EMPTY
        var displayedFluid = FluidStack.EMPTY
        var displayedAmount = 0L

        fun refreshResourceSlot() {
            itemTemplateSlot.style { it.zIndex(if (displayedItem.isEmpty) 0 else 1) }
            fluidTemplateSlot.style { it.zIndex(if (displayedFluid.isEmpty) 0 else 1) }
            val resourceName =
                when {
                    !displayedItem.isEmpty -> displayedItem.hoverName
                    !displayedFluid.isEmpty -> displayedFluid.hoverName
                    else -> null
                }
            val tooltip =
                if (resourceName == null) {
                    Component.translatable("gui.lazy.replicator.resource.empty")
                } else {
                    Component.translatable("gui.lazy.replicator.resource.selected", resourceName, displayedAmount)
                }
            itemTemplateSlot.style { it.tooltips(tooltip) }
            fluidTemplateSlot.style { it.tooltips(tooltip) }
        }
        itemTemplateSlot.registerValueListener { template ->
            displayedItem = template
            refreshResourceSlot()
        }
        fluidTemplateSlot.registerValueListener { template ->
            displayedFluid = template
            refreshResourceSlot()
        }

        val amountValue = BindableValue(0L)
        amountValue.setDisplay(false)
        amountValue.registerValueListener { amount ->
            displayedAmount = amount
            amountButton.setText(
                if (amount > 0L) {
                    Component.literal("× ${CompactLongFormatter.format(amount)}")
                } else {
                    Component.literal("—")
                },
            )
            amountButton.setActive(amount > 0L)
            amountEditor.setEnabled(amount > 0L)
            refreshResourceSlot()
        }
        amountValue.bind(
            DataBindingBuilder
                .longValS2C(model::amount)
                .initialValue(0L)
                .build(),
        )
        root.addChild(amountValue)

        val displayedGear = BindableValue(ReplicatorGear.DEFAULT)
        displayedGear.setDisplay(false)
        displayedGear.registerValueListener { gear ->
            gearButton.setText(Component.translatable("gui.lazy.replicator.interval", gear.intervalTicks))
        }
        displayedGear.bind(
            DataBindingBuilder
                .enumValS2C(ReplicatorGear::class.java, model::gear)
                .initialValue(ReplicatorGear.DEFAULT)
                .build(),
        )
        root.addChild(displayedGear)
        root.addChild(amountEditor.dialog)
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

    private fun UIElement.installResourceInteraction(
        model: ReplicatorUiModel,
        amountEditor: AmountEditor,
    ) {
        addEventListener(
            "mouseDown",
            { event ->
                if (event.button == RIGHT_MOUSE_BUTTON || event.button == MIDDLE_MOUSE_BUTTON) {
                    amountEditor.open()
                    event.stopImmediatePropagation()
                }
            },
            true,
        )
        addServerEventListener("mouseDown") { event ->
            if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                model.markCarriedStackOrClear()
            }
        }
    }

    private class AmountEditor(
        private val model: ReplicatorUiModel,
    ) {
        val dialog: Dialog
        private val amountField: TextField
        private var enabled = false

        init {
            lateinit var field: TextField
            val content =
                element(
                    {
                        cls = { +"lazy-replicator__amount-editor" }
                    },
                ) {
                    label(
                        {
                            text = Component.translatable("gui.lazy.replicator.amount.title")
                            cls = { +"lazy-replicator__amount-title" }
                        },
                    )
                    row(
                        {
                            cls = { +"lazy-replicator__amount-row" }
                        },
                    ) {
                        amountStepButton("−", -1L)
                        field =
                            textField(
                                {
                                    text = "1"
                                    cls = { +"lazy-replicator__amount-field" }
                                },
                            ) {
                                asNumeric(1L, Long.MAX_VALUE)
                            }.element.apply {
                                bind(
                                    DataBindingBuilder
                                        .string(model::amountText, model::setAmountText)
                                        .initialValue("1")
                                        .build(),
                                )
                            }
                        amountStepButton("+", 1L)
                    }
                    button(
                        {
                            text = Component.translatable("gui.done")
                            cls = { +"lazy-replicator__amount-done" }
                            onClick = { close() }
                        },
                    )
                }
            amountField = field

            dialog =
                Dialog()
                    .setAutoClose(false)
                    .setClickOutsideClose(false)
                    .darkenBackground()
                    .apply {
                        style { it.zIndex(2) }
                        width(TaffyDimension.maxContent())
                    }
            dialog.titleBar.setDisplay(false)
            dialog.buttonContainer.setDisplay(false)
            dialog.addContent(content)
            dialog.setDisplay(false)
            dialog.addEventListener("keyDown") { event ->
                if (event.keyCode == KEY_E || event.keyCode == KEY_ESCAPE) {
                    close()
                    event.stopPropagation()
                }
            }
        }

        private fun UIContainer<*, *>.amountStepButton(
            buttonText: String,
            direction: Long,
        ) {
            button(
                {
                    text = Component.literal(buttonText)
                    cls = { +"lazy-replicator__amount-step" }
                    style = {
                        tooltips(
                            Component.translatable(
                                if (direction < 0L) {
                                    "gui.lazy.replicator.amount.decrease"
                                } else {
                                    "gui.lazy.replicator.amount.increase"
                                },
                            ),
                        )
                    }
                    onServerClick = { event ->
                        if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                            model.adjustAmount(direction)
                        }
                    }
                },
            )
        }

        fun open() {
            if (!enabled) return
            dialog.getModularUI()?.apply {
                shouldCloseOnEsc(false)
                shouldCloseOnKeyInventory(false)
            }
            dialog.setDisplay(true)
            dialog.focus()
            amountField.focus()
        }

        fun setEnabled(enabled: Boolean) {
            this.enabled = enabled
        }

        private fun close() {
            dialog.setDisplay(false)
            dialog.getModularUI()?.apply {
                clearFocus()
                shouldCloseOnEsc(true)
                shouldCloseOnKeyInventory(true)
            }
        }
    }

    private class ReplicatorUiModel(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) : IoPanelModel {
        override val player = holder.player

        override val editor
            get() = blockEntity?.ioController

        private val blockEntity: ReplicatorBlockEntity?
            get() =
                holder.player.level().blockEntityOrNull(
                    holder.pos,
                    ReplicatorRegistries.blockEntity.get(),
                )

        fun itemTemplate(): ItemStack = blockEntity?.getItemTemplate() ?: ItemStack.EMPTY

        fun setItemTemplate(stack: ItemStack) {
            if (!isValid()) return
            blockEntity?.setItemTemplate(stack)
        }

        fun fluidTemplate(): FluidStack = blockEntity?.getFluidTemplate() ?: FluidStack.EMPTY

        fun setFluidTemplate(stack: FluidStack) {
            if (!isValid()) return
            blockEntity?.setFluidTemplate(stack)
        }

        fun amount(): Long = blockEntity?.getResource()?.amount ?: 0L

        fun amountText(): String = amount().coerceAtLeast(1L).toString()

        fun setAmountText(text: String) {
            if (!isValid()) return
            text.toLongOrNull()?.takeIf { it > 0L }?.let { blockEntity?.setAmount(it) }
        }

        fun adjustAmount(direction: Long) {
            if (!isValid()) return
            val entity = blockEntity ?: return
            entity.adjustAmount(entity.amountStep() * direction)
        }

        fun gear(): ReplicatorGear = blockEntity?.getGear() ?: ReplicatorGear.DEFAULT

        fun markCarriedStackOrClear() {
            val stack = holder.player.containerMenu.carried
            if (stack.isEmpty) {
                blockEntity?.clearResource()
            } else {
                setItemTemplate(stack)
            }
        }

        fun cycleGear() {
            val entity = blockEntity ?: return
            if (!isValid()) return
            entity.cycleGear()
        }

        override fun isValid(): Boolean {
            val block = holder.blockState.block as? ReplicatorBlock ?: return false
            return block.stillValid(holder)
        }
    }

    private const val LEFT_MOUSE_BUTTON = 0
    private const val RIGHT_MOUSE_BUTTON = 1
    private const val MIDDLE_MOUSE_BUTTON = 2
    private const val KEY_E = 69
    private const val KEY_ESCAPE = 256
}
