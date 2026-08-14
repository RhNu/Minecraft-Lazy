package rhx.lazy.core.io

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import rhx.lazy.core.displayActionBar
import java.util.concurrent.CopyOnWriteArrayList

internal object ConfigurationCardData {
    fun get(stack: ItemStack): IoConfiguration {
        if (!ConfigurationCardRegistries.isCard(stack)) return IoConfiguration.DEFAULT
        val root = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return IoConfiguration.DEFAULT
        return if (root.contains(CONFIGURATION_TAG, Tag.TAG_COMPOUND.toInt())) {
            IoConfiguration.load(root.getCompound(CONFIGURATION_TAG))
        } else {
            IoConfiguration.DEFAULT
        }
    }

    fun set(
        stack: ItemStack,
        configuration: IoConfiguration,
    ) {
        require(ConfigurationCardRegistries.isCard(stack)) { "Cannot store IO configuration on a non-card item" }
        val root = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()
        root.put(CONFIGURATION_TAG, configuration.save())
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root))
    }

    private const val CONFIGURATION_TAG = "lazyIoConfiguration"
}

internal fun interface ConfigurationCardSource {
    fun findCards(player: ServerPlayer): List<ItemStack>
}

internal object ConfigurationCardSources {
    private val externalSources = CopyOnWriteArrayList<ConfigurationCardSource>()

    fun register(source: ConfigurationCardSource) {
        externalSources += source
    }

    fun allCards(player: ServerPlayer): List<ItemStack> {
        val inventoryCards =
            buildList {
                add(player.offhandItem)
                addAll(player.inventory.items)
            }.filter(ConfigurationCardRegistries::isCard)
        return inventoryCards + externalSources.flatMap { it.findCards(player) }.filter(ConfigurationCardRegistries::isCard)
    }

    fun selectConfiguration(player: ServerPlayer): ConfigurationCardSelection {
        val configurations = allCards(player).map(ConfigurationCardData::get).distinct()
        return when (configurations.size) {
            0 -> ConfigurationCardSelection.Missing
            1 -> ConfigurationCardSelection.Selected(configurations.single())
            else -> ConfigurationCardSelection.Ambiguous
        }
    }
}

internal sealed interface ConfigurationCardSelection {
    data class Selected(
        val configuration: IoConfiguration,
    ) : ConfigurationCardSelection

    data object Missing : ConfigurationCardSelection

    data object Ambiguous : ConfigurationCardSelection
}

internal fun applyConfigurationCardOnPlacement(
    player: Player?,
    blockEntity: IoManagedBlockEntity,
) {
    val serverPlayer = player as? ServerPlayer ?: return
    when (val selection = ConfigurationCardSources.selectConfiguration(serverPlayer)) {
        is ConfigurationCardSelection.Selected -> blockEntity.ioController.applyConfiguration(selection.configuration)
        ConfigurationCardSelection.Missing -> Unit
        ConfigurationCardSelection.Ambiguous ->
            serverPlayer.displayActionBar("message.lazy.configuration_card.ambiguous")
    }
}
