package rhx.lazy.ui

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
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
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import rhx.lazy.block.BufferBlock
import rhx.lazy.block.entity.BufferBlockEntity
import rhx.lazy.util.lazyId

internal object BufferUI {
    private val stylesheet = lazyId("lss/buffer.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        lateinit var confirmationLayer: UIElement
        lateinit var clearButton: com.lowdragmc.lowdraglib2.gui.ui.elements.Button

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
                        DataBindingBuilder
                            .componentS2C {
                                Component.translatable(
                                    "gui.lazy.buffer.summary",
                                    blockEntity(holder)?.totalItemCount ?: 0,
                                    BufferBlockEntity.TOTAL_ITEM_CAPACITY,
                                    blockEntity(holder)?.totalFluidAmount ?: 0,
                                    BufferBlockEntity.TOTAL_FLUID_CAPACITY,
                                )
                            }.build(),
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
                        column(
                            {
                                cls = { +"lazy-buffer__item" }
                            },
                        ) {
                            itemSlot {
                                bind(
                                    DataBindingBuilder
                                        .itemStackS2C {
                                            blockEntity(holder)?.getItemTemplate(slot) ?: ItemStack.EMPTY
                                        }.build(),
                                )
                                withTooltips()
                                asXeiRecipeIngredient(IngredientIO.NONE)
                            }
                            label(
                                {
                                    cls = { +"lazy-buffer__amount" }
                                },
                            ) {
                                bind(
                                    DataBindingBuilder
                                        .componentS2C {
                                            Component.translatable(
                                                "gui.lazy.buffer.item_count",
                                                blockEntity(holder)?.getItemCount(slot) ?: 0,
                                            )
                                        }.build(),
                                )
                            }
                        }
                    }
                }

                label(
                    {
                        text = Component.translatable("gui.lazy.buffer.fluids")
                        cls = { +"lazy-buffer__section-title" }
                    },
                )

                column(
                    {
                        cls = { +"lazy-buffer__fluids" }
                    },
                ) {
                    repeat(2) { rowIndex ->
                        row(
                            {
                                cls = { +"lazy-buffer__fluid-row" }
                            },
                        ) {
                            repeat(2) { columnIndex ->
                                val tank = rowIndex * 2 + columnIndex
                                column(
                                    {
                                        cls = { +"lazy-buffer__fluid" }
                                    },
                                ) {
                                    label(
                                        {
                                            cls = { +"lazy-buffer__fluid-name" }
                                        },
                                    ) {
                                        bind(
                                            DataBindingBuilder
                                                .componentS2C {
                                                    fluidName(blockEntity(holder)?.getFluid(tank) ?: FluidStack.EMPTY)
                                                }.build(),
                                        )
                                    }
                                    fluidSlot(
                                        {
                                            capacity = BufferBlockEntity.FLUID_TANK_CAPACITY
                                            allowClickFilled = false
                                            allowClickDrained = false
                                            bind(fluidHandler(holder), tank)
                                        },
                                    ) {
                                        withTooltips()
                                        asXeiRecipeIngredient(IngredientIO.NONE)
                                    }
                                }
                            }
                        }
                    }
                }

                row(
                    {
                        cls = { +"lazy-buffer__actions" }
                    },
                ) {
                    clearButton =
                        button(
                            {
                                text = Component.translatable("gui.lazy.buffer.clear")
                                active = false
                                cls = { +"lazy-buffer__clear" }
                                onClick = { confirmationLayer.setVisible(true) }
                            },
                        ).element
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
                                            if (isValid(holder)) {
                                                blockEntity(holder)?.clearContents()
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
            DataBindingBuilder
                .boolS2C { blockEntity(holder)?.hasContents() == true }
                .initialValue(false)
                .build(),
        )
        root.addChild(hasContents)

        return ModularUI(
            UI.of(
                root,
                StylesheetManager.MC,
                stylesheet,
            ),
            holder.player,
        )
    }

    private fun blockEntity(holder: BlockUIMenuType.BlockUIHolder): BufferBlockEntity? =
        holder.player.level().getBlockEntity(holder.pos) as? BufferBlockEntity

    private fun fluidHandler(holder: BlockUIMenuType.BlockUIHolder): IFluidHandler = blockEntity(holder)?.fluidHandler ?: EmptyFluidHandler

    private fun isValid(holder: BlockUIMenuType.BlockUIHolder): Boolean {
        val block = holder.blockState.block as? BufferBlock ?: return false
        return block.stillValid(holder)
    }

    private fun fluidName(fluid: FluidStack): Component =
        if (fluid.isEmpty) {
            Component.translatable("gui.lazy.buffer.empty")
        } else {
            fluid.hoverName
        }

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
}
