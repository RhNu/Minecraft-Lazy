package rhx.lazy.feature.repairer

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.acceptQuickMove
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
import rhx.lazy.core.lazyId

internal object RepairerUI {
    private val stylesheet = lazyId("lss/repairer.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        val model = RepairerUiModel(holder)
        lateinit var repairButton: Button

        val root =
            element(
                {
                    cls = {
                        +"panel_bg"
                        +"lazy-repairer"
                    }
                },
            ) {
                label(
                    {
                        text = Component.translatable("block.lazy.repairer")
                        cls = { +"lazy-repairer__title" }
                    },
                )

                row(
                    {
                        cls = { +"lazy-repairer__controls" }
                    },
                ) {
                    itemSlot(
                        {
                            bind(model.itemHandler, 0)
                            cls = { +"lazy-repairer__slot" }
                        },
                    ) {
                        withTooltips()
                        acceptQuickMove()
                        asXeiRecipeIngredient(IngredientIO.NONE)
                    }

                    repairButton =
                        button(
                            {
                                noText()
                                cls = { +"lazy-repairer__button" }
                                style = {
                                    tooltips(Component.translatable("gui.lazy.repairer.repair"))
                                }
                                onServerClick = { event ->
                                    if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                        model.repairOnce()
                                    }
                                }
                            },
                        ).element.apply {
                            addPreIcon(ItemStackTexture(ItemStack(Items.ANVIL)))
                        }
                }

                label(
                    {
                        text = Component.translatable("container.inventory")
                        cls = { +"lazy-repairer__inventory-title" }
                    },
                )

                inventorySlots(
                    {
                        cls = { +"lazy-repairer__inventory" }
                    },
                )
            }

        val canRepair = BindableValue(false)
        canRepair.setDisplay(false)
        canRepair.registerValueListener(repairButton::setActive)
        canRepair.bind(booleanBinding(model::hasRepairableItem))
        root.addChild(canRepair)

        return ModularUI(
            UI.of(
                root,
                StylesheetManager.MC,
                stylesheet,
            ),
            holder.player,
        )
    }

    private class RepairerUiModel(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) {
        private val blockEntity: RepairerBlockEntity?
            get() =
                holder.player.level().blockEntityOrNull(
                    holder.pos,
                    RepairerRegistries.blockEntity.get(),
                )

        val itemHandler: IItemHandlerModifiable
            get() = blockEntity?.itemHandler ?: EmptyItemHandler

        fun hasRepairableItem(): Boolean = blockEntity?.hasRepairableItem() == true

        fun repairOnce() {
            val entity = blockEntity ?: return
            if (!isValid()) return
            entity.repairOnce(
                RepairerConfigs.settings.minimumRepairPercent.get(),
                RepairerConfigs.settings.maximumRepairPercent.get(),
                holder.player.random,
                holder.player,
            )
        }

        fun isValid(): Boolean {
            val block = holder.blockState.block as? RepairerBlock ?: return false
            return block.stillValid(holder)
        }
    }

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

        override fun getSlotLimit(slot: Int): Int = 1

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
}
