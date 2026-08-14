package rhx.lazy.integration.ae2

import appeng.api.features.IGridLinkableHandler
import appeng.api.ids.AEComponents
import net.minecraft.core.GlobalPos
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.io.ConfigurationCardData
import rhx.lazy.core.io.ConfigurationCardRegistries
import rhx.lazy.core.io.IoMode

internal object ConfigurationCardLinking {
    val handler: IGridLinkableHandler =
        object : IGridLinkableHandler {
            override fun canLink(stack: ItemStack): Boolean = ConfigurationCardRegistries.isCard(stack)

            override fun link(
                itemStack: ItemStack,
                pos: GlobalPos,
            ) {
                itemStack.set(AEComponents.WIRELESS_LINK_TARGET, pos)
                val current = ConfigurationCardData.get(itemStack)
                ConfigurationCardData.set(
                    itemStack,
                    current.copy(mode = IoMode.NETWORK, networkTarget = Ae2NetworkOutputProvider.createTarget(pos)),
                )
            }

            override fun unlink(itemStack: ItemStack) {
                itemStack.remove(AEComponents.WIRELESS_LINK_TARGET)
                val current = ConfigurationCardData.get(itemStack)
                if (current.networkTarget?.providerId == Ae2NetworkOutputProvider.ID) {
                    ConfigurationCardData.set(itemStack, current.copy(mode = IoMode.PASSIVE, networkTarget = null))
                }
            }
        }
}

internal sealed interface ConfigurationCardTargetSelection<out T> {
    data class Selected<T>(
        val target: T,
    ) : ConfigurationCardTargetSelection<T>

    data object Missing : ConfigurationCardTargetSelection<Nothing>

    data object Ambiguous : ConfigurationCardTargetSelection<Nothing>

    companion object {
        fun <T> select(
            handTargets: List<T?>,
            inventoryTargets: List<T>,
        ): ConfigurationCardTargetSelection<T> {
            handTargets.firstNotNullOfOrNull { it }?.let { return Selected(it) }
            val distinctTargets = inventoryTargets.distinct()
            return when (distinctTargets.size) {
                0 -> Missing
                1 -> Selected(distinctTargets.single())
                else -> Ambiguous
            }
        }
    }
}
