package rhx.lazy.core.io

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.inventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import net.minecraft.world.item.ItemStack

internal object ConfigurationCardUI {
    fun create(holder: HeldItemUIMenuType.HeldItemUIHolder): ModularUI {
        val model = Model(holder)
        lateinit var installIoPanel: (UIElement) -> Unit
        val root =
            element(
                {
                    cls = {
                        +"panel_bg"
                        +"lazy-configuration-card"
                    }
                },
            ) {
                installIoPanel = IoPanelUI.addIoPanel(this, model)
                inventorySlots({ cls = { +"lazy-configuration-card__inventory" } })
            }
        installIoPanel(root)
        return ModularUI(UI.of(root, StylesheetManager.MC, IoPanelUI.stylesheet), holder.player)
    }

    private class Model(
        private val holder: HeldItemUIMenuType.HeldItemUIHolder,
    ) : IoPanelModel {
        override val player = holder.player
        override val editor: IoConfigurationEditor = CardEditor(holder.itemStack)

        override fun isValid(): Boolean = ConfigurationCardRegistries.isCard(holder.itemStack)
    }

    private class CardEditor(
        private val stack: ItemStack,
    ) : IoConfigurationEditor {
        override val configuration: IoConfiguration
            get() = ConfigurationCardData.get(stack)

        override val capabilities: Set<NetworkInsertCapability>
            get() = NetworkInsertCapabilities.all

        override fun setMode(mode: IoMode) {
            update(configuration.copy(mode = mode))
        }

        override fun cycleSide(side: RelativeSide) {
            val current = configuration
            update(current.withSide(side, current.side(side).next()))
        }

        override fun toggleAutoEject() {
            val current = configuration
            update(current.copy(autoEject = !current.autoEject))
        }

        override fun setNetworkTarget(target: NetworkTargetRef): Boolean {
            val provider = NetworkOutputProviders.get(target.providerId) ?: return false
            if (!provider.isTargetValid(target)) return false
            update(configuration.copy(mode = IoMode.NETWORK, networkTarget = target.copy()))
            return true
        }

        private fun update(configuration: IoConfiguration) {
            ConfigurationCardData.set(stack, configuration)
        }
    }
}
