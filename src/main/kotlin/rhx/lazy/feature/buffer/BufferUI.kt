package rhx.lazy.feature.buffer

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.column
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.asXeiRecipeIngredient
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.fluidSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.itemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.elements.withTooltips
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
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.io.IoPanelModel
import rhx.lazy.core.io.IoPanelUI
import rhx.lazy.core.lazyId

internal object BufferUI {
    private val stylesheet = lazyId("lss/buffer.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        lateinit var confirmationLayer: UIElement
        lateinit var clearButton: UIElement
        val model = BufferUiModel(holder)

        val root =
            element(
                {
                    cls = {
                        +"panel_bg"
                        +"lazy-buffer"
                    }
                },
            ) {
                label(
                    {
                        text = Component.translatable("block.lazy.buffer")
                        cls = { +"lazy-buffer__title" }
                    },
                )

                label(
                    {
                        cls = { +"lazy-buffer__summary" }
                    },
                ) {
                    bind(
                        componentBinding {
                            Component.translatable(
                                "gui.lazy.buffer.summary",
                                model.totalItemCount,
                                BufferBlockEntity.TOTAL_ITEM_CAPACITY,
                                model.totalFluidAmount,
                                BufferBlockEntity.TOTAL_FLUID_CAPACITY,
                            )
                        },
                    )
                }

                label(
                    {
                        text = Component.translatable("gui.lazy.buffer.items")
                        cls = { +"lazy-buffer__section-title" }
                    },
                )

                row(
                    {
                        cls = { +"lazy-buffer__items" }
                    },
                ) {
                    repeat(BufferBlockEntity.ITEM_SLOT_COUNT) { slot ->
                        itemSlot(
                            {
                                cls = { +"lazy-buffer__item-slot" }
                            },
                        ) {
                            bind(
                                itemStackBinding { model.itemStack(slot) },
                            )
                            withTooltips()
                            asXeiRecipeIngredient(IngredientIO.NONE)
                        }
                    }
                }

                label(
                    {
                        text = Component.translatable("gui.lazy.buffer.fluids")
                        cls = { +"lazy-buffer__section-title" }
                    },
                )

                row(
                    {
                        cls = { +"lazy-buffer__fluids" }
                    },
                ) {
                    repeat(BufferBlockEntity.FLUID_TANK_COUNT) { tank ->
                        fluidSlot(
                            {
                                capacity = BufferBlockEntity.FLUID_TANK_CAPACITY
                                allowClickFilled = false
                                allowClickDrained = false
                                bind(model.fluidHandler, tank)
                                cls = { +"lazy-buffer__fluid" }
                            },
                        ) {
                            withTooltips()
                            asXeiRecipeIngredient(IngredientIO.NONE)
                        }
                    }
                }

                row(
                    {
                        cls = { +"lazy-buffer__actions" }
                    },
                ) {
                    IoPanelUI.addIoControl(this, model)
                    clearButton =
                        button(
                            {
                                noText()
                                active = false
                                cls = { +"lazy-buffer__icon-button" }
                                style = {
                                    tooltips(Component.translatable("gui.lazy.buffer.clear"))
                                }
                                onClick = { confirmationLayer.setVisible(true) }
                            },
                        ).element.apply {
                            addPreIcon(ItemStackTexture(ItemStack(Items.BARRIER)))
                        }
                }

                confirmationLayer =
                    element(
                        {
                            visible = false
                            cls = { +"lazy-buffer__confirmation-layer" }
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
                                    +"lazy-buffer__confirmation"
                                }
                            },
                        ) {
                            label(
                                {
                                    text = Component.translatable("gui.lazy.buffer.confirm.title")
                                    cls = { +"lazy-buffer__confirmation-title" }
                                },
                            )
                            label(
                                {
                                    text = Component.translatable("gui.lazy.buffer.confirm.description")
                                    cls = { +"lazy-buffer__confirmation-description" }
                                },
                            )
                            row(
                                {
                                    cls = { +"lazy-buffer__confirmation-actions" }
                                },
                            ) {
                                button(
                                    {
                                        text = Component.translatable("gui.lazy.buffer.confirm")
                                        cls = { +"lazy-buffer__confirmation-button" }
                                        onClick = { confirmationLayer.setVisible(false) }
                                        onServerClick = {
                                            if (model.isValid()) {
                                                model.clearContents()
                                            }
                                        }
                                    },
                                )
                                button(
                                    {
                                        text = Component.translatable("gui.lazy.buffer.cancel")
                                        cls = { +"lazy-buffer__confirmation-button" }
                                        onClick = { confirmationLayer.setVisible(false) }
                                    },
                                )
                            }
                        }
                    }.element
            }

        val hasContents = BindableValue(false)
        hasContents.setDisplay(false)
        hasContents.registerValueListener(clearButton::setActive)
        hasContents.bind(
            booleanBinding(model::hasContents),
        )
        root.addChild(hasContents)

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

    private class BufferUiModel(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) : IoPanelModel {
        override val player = holder.player

        override val controller
            get() = blockEntity?.ioController

        private val blockEntity: BufferBlockEntity?
            get() =
                holder.player.level().blockEntityOrNull(
                    holder.pos,
                    BufferRegistries.blockEntity.get(),
                )

        val totalItemCount: Int
            get() = blockEntity?.totalItemCount ?: 0

        val totalFluidAmount: Int
            get() = blockEntity?.totalFluidAmount ?: 0

        val fluidHandler: IFluidHandler
            get() = blockEntity?.fluidHandler ?: EmptyFluidHandler

        fun itemStack(slot: Int): ItemStack {
            val entity = blockEntity ?: return ItemStack.EMPTY
            val template = entity.getItemTemplate(slot)
            val count = entity.getItemCount(slot)
            return if (template.isEmpty || count <= 0) ItemStack.EMPTY else template.copyWithCount(count)
        }

        fun hasContents(): Boolean = blockEntity?.hasContents() == true

        fun clearContents() {
            blockEntity?.clearContents()
        }

        override fun isValid(): Boolean {
            val block = holder.blockState.block as? BufferBlock ?: return false
            return block.stillValid(holder)
        }
    }

    private fun componentBinding(value: () -> Component) =
        DataBindingBuilder
            .componentS2C { value() }
            .build()

    private fun itemStackBinding(value: () -> ItemStack) =
        DataBindingBuilder
            .itemStackS2C { value() }
            .build()

    private fun booleanBinding(value: () -> Boolean) =
        DataBindingBuilder
            .boolS2C { value() }
            .initialValue(false)
            .build()

    private object EmptyFluidHandler : IFluidHandler {
        override fun getTanks(): Int = BufferBlockEntity.FLUID_TANK_COUNT

        override fun getFluidInTank(tank: Int): FluidStack = FluidStack.EMPTY

        override fun getTankCapacity(tank: Int): Int = BufferBlockEntity.FLUID_TANK_CAPACITY

        override fun isFluidValid(
            tank: Int,
            stack: FluidStack,
        ): Boolean = false

        override fun fill(
            resource: FluidStack,
            action: IFluidHandler.FluidAction,
        ): Int = 0

        override fun drain(
            resource: FluidStack,
            action: IFluidHandler.FluidAction,
        ): FluidStack = FluidStack.EMPTY

        override fun drain(
            maxDrain: Int,
            action: IFluidHandler.FluidAction,
        ): FluidStack = FluidStack.EMPTY
    }

    private const val LEFT_MOUSE_BUTTON = 0
}
