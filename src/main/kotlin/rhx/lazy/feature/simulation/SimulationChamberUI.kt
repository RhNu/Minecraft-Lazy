package rhx.lazy.feature.simulation

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
import com.lowdragmc.lowdraglib2.gui.ui.elements.progressBar
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

internal object SimulationChamberUI {
    private val stylesheet = lazyId("lss/simulation_chamber.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        val model = Model(holder)
        lateinit var outputMultiplier: UIElement
        lateinit var speedMultiplier: UIElement
        lateinit var pendingWarning: UIElement
        lateinit var installIo: (UIElement) -> Unit
        val root =
            element({
                cls = {
                    +"panel_bg"
                    +"lazy-simulation"
                }
            }) {
                label({
                    text = Component.translatable("block.lazy.simulation_chamber")
                    cls = { +"lazy-simulation__title" }
                })
                column({ cls = { +"lazy-simulation__machine" } }) {
                    row({ cls = { +"lazy-simulation__inputs" } }) {
                        inputSlot(model, SimulationChamberBlockEntity.TARGET_SLOT, "gui.lazy.simulation_chamber.target")
                        inputSlot(model, SimulationChamberBlockEntity.CORE_SLOT, "gui.lazy.simulation_chamber.core")
                    }
                    row({ cls = { +"lazy-simulation__processing" } }) {
                        outputMultiplier = multiplierIcon(Items.CHEST)
                        progressBar({
                            range(0f, 1f)
                            label(Component.empty())
                            cls = { +"lazy-simulation__progress" }
                            style = { tooltips(Component.translatable("gui.lazy.simulation_chamber.progress")) }
                        }) {
                            bind(DataBindingBuilder.floatValS2C(model::progress).initialValue(0f).build())
                        }
                        speedMultiplier = multiplierIcon(Items.CLOCK)
                    }
                    row({ cls = { +"lazy-simulation__actions" } }) {
                        installIo = IoPanelUI.addIoControl(this, model)
                        element({ cls = { +"lazy-simulation__action-placeholder" } }) {
                            pendingWarning =
                                button({
                                    visible = false
                                    active = false
                                    noText()
                                    cls = { +"lazy-simulation__icon-button" }
                                    style = { tooltips(Component.translatable("gui.lazy.simulation_chamber.pending")) }
                                }).element.apply {
                                    addPreIcon(ItemStackTexture(ItemStack(Items.BARRIER)))
                                }
                        }
                    }
                }
                label({
                    text = Component.translatable("container.inventory")
                    cls = { +"lazy-simulation__inventory-title" }
                })
                inventorySlots({ cls = { +"lazy-simulation__inventory" } })
            }

        bindTooltip(root, outputMultiplier, model::outputMultiplierTooltip)
        bindTooltip(root, speedMultiplier, model::speedMultiplierTooltip)
        val hasPending = BindableValue(false)
        hasPending.setDisplay(false)
        hasPending.registerValueListener(pendingWarning::setVisible)
        hasPending.bind(DataBindingBuilder.boolS2C(model::hasWaitingOutputs).initialValue(false).build())
        root.addChild(hasPending)
        installIo(root)
        return ModularUI(UI.of(root, StylesheetManager.MC, stylesheet, IoPanelUI.stylesheet), holder.player)
    }

    private fun com.lowdragmc.lowdraglib2.gui.ui.UIContainer<*, *>.inputSlot(
        model: Model,
        slot: Int,
        tooltip: String,
    ) {
        itemSlot({
            bind(model.inputHandler, slot)
            cls = { +"lazy-simulation__slot" }
            style = { tooltips(Component.translatable(tooltip)) }
        }) {
            withTooltips()
            acceptQuickMove()
            asXeiRecipeIngredient(IngredientIO.INPUT)
        }
    }

    private fun com.lowdragmc.lowdraglib2.gui.ui.UIContainer<*, *>.multiplierIcon(item: net.minecraft.world.item.Item): UIElement =
        button({
            active = false
            noText()
            cls = { +"lazy-simulation__icon-button" }
        }).element.apply {
            addPreIcon(ItemStackTexture(ItemStack(item)))
        }

    private fun bindTooltip(
        root: UIElement,
        element: UIElement,
        tooltip: () -> Component,
    ) {
        val value = BindableValue<Component>(Component.empty())
        value.setDisplay(false)
        value.registerValueListener { component -> element.style { style -> style.tooltips(component) } }
        value.bind(DataBindingBuilder.componentS2C(tooltip).build())
        root.addChild(value)
    }

    private class Model(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) : IoPanelModel {
        override val player = holder.player
        override val editor get() = blockEntity?.ioController
        private val blockEntity: SimulationChamberBlockEntity?
            get() = holder.player.level().blockEntityOrNull(holder.pos, SimulationRegistries.blockEntity.get())
        val inputHandler get() = blockEntity?.inputItemHandler ?: EmptyItems

        fun progress() = blockEntity?.progress() ?: 0f

        fun hasWaitingOutputs(): Boolean = blockEntity?.hasWaitingOutputs() == true

        fun outputMultiplierTooltip(): Component =
            Component.translatable("gui.lazy.simulation_chamber.output_multiplier", blockEntity?.outputMultiplier() ?: 0L)

        fun speedMultiplierTooltip(): Component =
            Component.translatable("gui.lazy.simulation_chamber.speed_multiplier", blockEntity?.speedMultiplier() ?: 0)

        override fun isValid() = (holder.blockState.block as? SimulationChamberBlock)?.stillValid(holder) == true
    }

    private object EmptyItems : IItemHandlerModifiable {
        override fun getSlots() = SimulationChamberBlockEntity.INPUT_SLOTS

        override fun getStackInSlot(slot: Int) = ItemStack.EMPTY

        override fun insertItem(
            slot: Int,
            stack: ItemStack,
            simulate: Boolean,
        ) = stack

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean,
        ) = ItemStack.EMPTY

        override fun getSlotLimit(slot: Int) = 64

        override fun isItemValid(
            slot: Int,
            stack: ItemStack,
        ) = false

        override fun setStackInSlot(
            slot: Int,
            stack: ItemStack,
        ) = Unit
    }
}
