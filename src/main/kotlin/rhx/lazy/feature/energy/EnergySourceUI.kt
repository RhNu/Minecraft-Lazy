package rhx.lazy.feature.energy

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.lazyId
import rhx.lazy.core.storage.NetworkStorage
import rhx.lazy.core.storage.NetworkStorageResult

internal object EnergySourceUI {
    private val stylesheet = lazyId("lss/energy_source.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        val model = EnergySourceUiModel(holder)
        val buttons = mutableMapOf<EnergyOutputMode, UIElement>()

        val root =
            element(
                {
                    cls = {
                        +"panel_bg"
                        +"lazy-energy-source"
                    }
                },
            ) {
                label(
                    {
                        text = Component.translatable("block.lazy.energy_source")
                        cls = { +"lazy-energy-source__title" }
                    },
                )

                row(
                    {
                        cls = { +"lazy-energy-source__modes" }
                    },
                ) {
                    modeButton(
                        buttons,
                        model,
                        EnergyOutputMode.PASSIVE,
                        EnergyRegistries.batteryItem.get(),
                        "gui.lazy.energy_source.passive",
                    )
                    modeButton(
                        buttons,
                        model,
                        EnergyOutputMode.ACTIVE,
                        Items.DISPENSER,
                        "gui.lazy.energy_source.active",
                    )
                    modeButton(
                        buttons,
                        model,
                        EnergyOutputMode.NETWORK,
                        Items.ENDER_CHEST,
                        "gui.lazy.energy_source.network",
                        NetworkStorage.isAvailable,
                    )
                }
            }

        val selectedMode = BindableValue(EnergyOutputMode.PASSIVE)
        selectedMode.setDisplay(false)
        selectedMode.registerValueListener { selected ->
            buttons.forEach { (mode, button) ->
                if (mode == selected) {
                    button.addClass(SELECTED_BUTTON_CLASS)
                } else {
                    button.removeClass(SELECTED_BUTTON_CLASS)
                }
            }
        }
        selectedMode.bind(
            DataBindingBuilder
                .enumValS2C(EnergyOutputMode::class.java, model::outputMode)
                .initialValue(EnergyOutputMode.PASSIVE)
                .build(),
        )
        root.addChild(selectedMode)

        return ModularUI(
            UI.of(
                root,
                StylesheetManager.MC,
                stylesheet,
            ),
            holder.player,
        )
    }

    private fun com.lowdragmc.lowdraglib2.gui.ui.UIContainer<*, *>.modeButton(
        buttons: MutableMap<EnergyOutputMode, UIElement>,
        model: EnergySourceUiModel,
        mode: EnergyOutputMode,
        icon: Item,
        translationKey: String,
        available: Boolean = true,
    ) {
        val button =
            button(
                {
                    active = available
                    noText()
                    cls = { +"lazy-energy-source__mode" }
                    style = {
                        tooltips(
                            Component.translatable(translationKey),
                            Component.translatable("$translationKey.description"),
                        )
                    }
                    onServerClick = { event ->
                        if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                            model.selectMode(mode)
                        }
                    }
                },
            ).element.apply {
                addPreIcon(ItemStackTexture(ItemStack(icon)))
            }
        buttons[mode] = button
    }

    private class EnergySourceUiModel(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) {
        private val blockEntity: EnergySourceBlockEntity?
            get() =
                holder.player.level().blockEntityOrNull(
                    holder.pos,
                    EnergyRegistries.sourceBlockEntity.get(),
                )

        fun outputMode(): EnergyOutputMode = blockEntity?.outputMode() ?: EnergyOutputMode.PASSIVE

        fun selectMode(mode: EnergyOutputMode) {
            val entity = blockEntity ?: return
            if (entity.outputMode() == mode) return

            if (mode != EnergyOutputMode.NETWORK) {
                entity.setOutputMode(mode)
                return
            }

            val player = holder.player as? ServerPlayer ?: return
            when (val result = NetworkStorage.primaryNetwork(player)) {
                is NetworkStorageResult.Success -> entity.setOutputMode(mode, result.value)
                NetworkStorageResult.NetworkNotFound ->
                    player.displayActionBar("message.lazy.beyond_dimensions.no_primary_network")

                else -> player.displayActionBar("message.lazy.beyond_dimensions.unavailable")
            }
        }

        fun isValid(): Boolean {
            val block = holder.blockState.block as? EnergySourceBlock ?: return false
            return block.stillValid(holder)
        }
    }

    private const val LEFT_MOUSE_BUTTON = 0
    private const val SELECTED_BUTTON_CLASS = "lazy-energy-source__mode--selected"
}
