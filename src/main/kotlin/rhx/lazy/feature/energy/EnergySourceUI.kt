package rhx.lazy.feature.energy

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import net.minecraft.network.chat.Component
import rhx.lazy.core.blockEntityOrNull
import rhx.lazy.core.io.IoPanelModel
import rhx.lazy.core.io.IoPanelUI
import rhx.lazy.core.lazyId

internal object EnergySourceUI {
    private val stylesheet = lazyId("lss/energy_source.lss")

    fun create(holder: BlockUIMenuType.BlockUIHolder): ModularUI {
        val model = EnergySourceUiModel(holder)

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
                        cls = { +"lazy-energy-source__actions" }
                    },
                ) {
                    IoPanelUI.addIoControl(this, model)
                }
            }

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

    private class EnergySourceUiModel(
        private val holder: BlockUIMenuType.BlockUIHolder,
    ) : IoPanelModel {
        override val player = holder.player

        override val controller
            get() = blockEntity?.ioController

        private val blockEntity: EnergySourceBlockEntity?
            get() =
                holder.player.level().blockEntityOrNull(
                    holder.pos,
                    EnergyRegistries.sourceBlockEntity.get(),
                )

        override fun isValid(): Boolean {
            val block = holder.blockState.block as? EnergySourceBlock ?: return false
            return block.stillValid(holder)
        }
    }
}
