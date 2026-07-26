package rhx.lazy.integration.botanypots

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
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.items.IItemHandlerModifiable
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.lazyId
import rhx.lazy.core.storage.NetworkStorage
import rhx.lazy.core.storage.NetworkStorageResult

internal object PlanterUI {
    private val stylesheet = lazyId("lss/planter.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        val model = PlanterUiModel(holder)
        val networkControlsVisible = NetworkStorage.isAvailable
        lateinit var networkButton: UIElement
        lateinit var downwardButton: UIElement
        lateinit var pendingWarning: UIElement

        val root =
            element(
                {
                    cls = {
                        +"panel_bg"
                        +"lazy-planter"
                    }
                },
            ) {
                label(
                    {
                        text = Component.translatable("block.lazy.planter")
                        cls = { +"lazy-planter__title" }
                    },
                )

                row(
                    {
                        cls = { +"lazy-planter__machine" }
                    },
                ) {
                    column(
                        {
                            cls = { +"lazy-planter__inputs" }
                        },
                    ) {
                        inputSlot(model, PlanterBlockEntity.SEED_SLOT, "gui.lazy.planter.seed")
                        inputSlot(model, PlanterBlockEntity.SOIL_SLOT, "gui.lazy.planter.soil")
                        inputSlot(model, PlanterBlockEntity.POT_SLOT, "gui.lazy.planter.pot")
                    }

                    column(
                        {
                            cls = { +"lazy-planter__center" }
                        },
                    ) {
                        progressBar(
                            {
                                range(0f, 1f)
                                label(Component.empty())
                                cls = { +"lazy-planter__progress" }
                            },
                        ) {
                            bind(
                                DataBindingBuilder
                                    .floatValS2C(model::progress)
                                    .initialValue(0f)
                                    .build(),
                            )
                        }
                        row(
                            {
                                cls = { +"lazy-planter__actions" }
                            },
                        ) {
                            networkButton =
                                button(
                                    {
                                        visible = networkControlsVisible
                                        noText()
                                        cls = {
                                            +"lazy-planter__icon-button"
                                            +"lazy-planter__toggle-button"
                                        }
                                        onServerClick = { event ->
                                            if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                                model.toggleNetworkForwarding()
                                            }
                                        }
                                    },
                                ).element.apply {
                                    addPreIcon(ItemStackTexture(ItemStack(Items.ENDER_CHEST)))
                                }

                            downwardButton =
                                button(
                                    {
                                        noText()
                                        cls = {
                                            +"lazy-planter__icon-button"
                                            +"lazy-planter__toggle-button"
                                        }
                                        onServerClick = { event ->
                                            if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                                model.toggleDownwardOutput()
                                            }
                                        }
                                    },
                                ).element.apply {
                                    addPreIcon(ItemStackTexture(ItemStack(Items.HOPPER)))
                                }

                            element(
                                {
                                    cls = { +"lazy-planter__action-placeholder" }
                                },
                            ) {
                                pendingWarning =
                                    button(
                                        {
                                            visible = false
                                            active = false
                                            noText()
                                            cls = { +"lazy-planter__icon-button" }
                                        },
                                    ).element.apply {
                                        addPreIcon(ItemStackTexture(ItemStack(Items.BARRIER)))
                                    }
                            }
                        }
                    }

                    column(
                        {
                            cls = { +"lazy-planter__outputs" }
                        },
                    ) {
                        repeat(3) { rowIndex ->
                            row(
                                {
                                    cls = { +"lazy-planter__output-row" }
                                },
                            ) {
                                repeat(4) { columnIndex ->
                                    val slot = rowIndex * 4 + columnIndex
                                    itemSlot(
                                        {
                                            bind(model.outputHandler, slot)
                                            cls = { +"lazy-planter__slot" }
                                        },
                                    ) {
                                        withTooltips()
                                        acceptQuickMove()
                                        asXeiRecipeIngredient(IngredientIO.OUTPUT)
                                    }
                                }
                            }
                        }
                    }
                }

                label(
                    {
                        text = Component.translatable("container.inventory")
                        cls = { +"lazy-planter__inventory-title" }
                    },
                )
                inventorySlots(
                    {
                        cls = { +"lazy-planter__inventory" }
                    },
                )
            }

        bindToggleButtonState(
            root,
            networkButton,
            model::isNetworkForwardingEnabled,
            "gui.lazy.planter.network",
        )
        bindToggleButtonState(
            root,
            downwardButton,
            model::isDownwardOutputEnabled,
            "gui.lazy.planter.downward",
        )

        val hasPending = BindableValue(false)
        hasPending.setDisplay(false)
        hasPending.registerValueListener(pendingWarning::setVisible)
        hasPending.bind(booleanBinding(model::hasPendingDrops))
        root.addChild(hasPending)

        val pendingTooltip = BindableValue<Tag>(CompoundTag())
        pendingTooltip.setDisplay(false)
        pendingTooltip.registerValueListener { tag ->
            pendingWarning.style { style ->
                style.tooltips(
                    *pendingTooltips(holder, tag as? CompoundTag ?: CompoundTag()).toTypedArray(),
                )
            }
        }
        pendingTooltip.bind(
            DataBindingBuilder
                .tagS2C(model::pendingTooltipTag)
                .initialValue(CompoundTag())
                .build(),
        )
        root.addChild(pendingTooltip)

        return ModularUI(
            UI.of(
                root,
                StylesheetManager.MC,
                stylesheet,
            ),
            holder.player,
        )
    }

    private fun com.lowdragmc.lowdraglib2.gui.ui.UIContainer<*, *>.inputSlot(
        model: PlanterUiModel,
        slot: Int,
        tooltipKey: String,
    ) {
        itemSlot(
            {
                bind(model.inputHandler, slot)
                cls = { +"lazy-planter__slot" }
                style = {
                    tooltips(Component.translatable(tooltipKey))
                }
            },
        ) {
            withTooltips()
            acceptQuickMove()
            asXeiRecipeIngredient(IngredientIO.INPUT)
        }
    }

    private fun bindToggleButtonState(
        root: UIElement,
        button: UIElement,
        state: () -> Boolean,
        labelKey: String,
    ) {
        val value = BindableValue(false)
        value.setDisplay(false)
        value.registerValueListener { enabled ->
            if (enabled) {
                button.addClass(ENABLED_BUTTON_CLASS)
            } else {
                button.removeClass(ENABLED_BUTTON_CLASS)
            }
            button.style { style ->
                style.tooltips(
                    Component.translatable(labelKey),
                    Component.translatable(
                        if (enabled) {
                            "gui.lazy.planter.enabled"
                        } else {
                            "gui.lazy.planter.disabled"
                        },
                    ),
                )
            }
        }
        value.bind(booleanBinding(state))
        root.addChild(value)
    }

    private fun pendingTooltips(
        holder: BlockUIMenuType.BlockUIHolder,
        tag: CompoundTag,
    ): List<Component> =
        buildList {
            add(Component.translatable("gui.lazy.planter.pending.title"))
            add(Component.translatable("gui.lazy.planter.pending.paused"))
            val entries =
                tag.getList(
                    PlanterOutputRouter.PENDING_ENTRIES_TAG,
                    Tag.TAG_COMPOUND.toInt(),
                )
            entries.forEach { raw ->
                val entry = raw as CompoundTag
                val stack =
                    ItemStack.parseOptional(
                        holder.player.registryAccess(),
                        entry.getCompound(PlanterOutputRouter.PENDING_STACK_TAG),
                    )
                if (!stack.isEmpty) {
                    add(
                        Component.translatable(
                            "gui.lazy.planter.pending.entry",
                            stack.hoverName,
                            entry.getLong(PlanterOutputRouter.PENDING_COUNT_TAG),
                        ),
                    )
                }
            }
            val remaining = tag.getInt(PlanterOutputRouter.PENDING_REMAINING_TYPES_TAG)
            if (remaining > 0) {
                add(Component.translatable("gui.lazy.planter.pending.more", remaining))
            }
        }

    private class PlanterUiModel(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) {
        private val blockEntity: PlanterBlockEntity?
            get() =
                holder.player.level().blockEntityOrNull(
                    holder.pos,
                    PlanterRegistries.blockEntity.get(),
                )

        val inputHandler: IItemHandlerModifiable
            get() = blockEntity?.inputHandler ?: EmptyItemHandler(PlanterBlockEntity.INPUT_SLOT_COUNT)

        val outputHandler: IItemHandlerModifiable
            get() = blockEntity?.outputHandler ?: EmptyItemHandler(PlanterBlockEntity.OUTPUT_SLOT_COUNT)

        fun progress(): Float = blockEntity?.progress() ?: 0f

        fun hasPendingDrops(): Boolean = blockEntity?.hasPendingDrops == true

        fun pendingTooltipTag(): Tag = blockEntity?.pendingTooltipTag() ?: CompoundTag()

        fun isNetworkForwardingEnabled(): Boolean = blockEntity?.isNetworkForwardingEnabled == true

        fun isDownwardOutputEnabled(): Boolean = blockEntity?.isDownwardOutputEnabled == true

        fun toggleDownwardOutput() {
            blockEntity?.toggleDownwardOutput()
        }

        fun toggleNetworkForwarding() {
            val entity = blockEntity ?: return
            val player = holder.player as? ServerPlayer ?: return
            if (entity.isNetworkForwardingEnabled) {
                entity.disableNetworkForwarding()
                player.displayActionBar(
                    "message.lazy.planter.network_forwarding",
                    Component.translatable("gui.lazy.planter.disabled"),
                )
                return
            }
            when (val result = NetworkStorage.primaryNetwork(player)) {
                is NetworkStorageResult.Success -> {
                    entity.enableNetworkForwarding(result.value)
                    if (entity.isNetworkForwardingEnabled) {
                        player.displayActionBar(
                            "message.lazy.planter.network_forwarding",
                            Component.translatable("gui.lazy.planter.enabled"),
                        )
                    } else {
                        player.displayActionBar("message.lazy.beyond_dimensions.unavailable")
                    }
                }

                NetworkStorageResult.NetworkNotFound ->
                    player.displayActionBar("message.lazy.beyond_dimensions.no_primary_network")

                else -> player.displayActionBar("message.lazy.beyond_dimensions.unavailable")
            }
        }

        fun isValid(): Boolean {
            val block = holder.blockState.block as? PlanterBlock ?: return false
            return block.stillValid(holder)
        }
    }

    private class EmptyItemHandler(
        private val size: Int,
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

        override fun getSlotLimit(slot: Int): Int = 64

        override fun isItemValid(
            slot: Int,
            stack: ItemStack,
        ): Boolean = false

        override fun setStackInSlot(
            slot: Int,
            stack: ItemStack,
        ) = Unit
    }

    private fun booleanBinding(value: () -> Boolean) =
        DataBindingBuilder
            .boolS2C { value() }
            .initialValue(false)
            .build()

    private const val LEFT_MOUSE_BUTTON = 0
    private const val ENABLED_BUTTON_CLASS = "lazy-planter__icon-button--enabled"
}
