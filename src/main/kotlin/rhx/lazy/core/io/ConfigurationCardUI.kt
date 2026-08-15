package rhx.lazy.core.io

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import net.minecraft.world.item.ItemStack

/** The card's own screen is the IO panel and nothing else: it holds no items to interact with. */
internal object ConfigurationCardUI {
    fun create(holder: HeldItemUIMenuType.HeldItemUIHolder): ModularUI = IoPanelUI.createStandaloneUI(Model(holder))

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
            update(configuration.copy(mode = IoMode.NETWORK, networkTarget = target.deepCopy()))
            return true
        }

        override fun clearNetworkTarget() {
            update(configuration.copy(mode = IoMode.PASSIVE, networkTarget = null))
        }

        private fun update(configuration: IoConfiguration) {
            ConfigurationCardData.set(stack, configuration)
        }
    }
}
