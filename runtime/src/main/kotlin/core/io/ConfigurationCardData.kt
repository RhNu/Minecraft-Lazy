package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import rhx.lazy.core.displayActionBar
import rhx.lazy.integration.api.LazyInternalApi
import java.util.concurrent.CopyOnWriteArrayList

@LazyInternalApi
public object ConfigurationCardData {
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

@LazyInternalApi
public fun interface ConfigurationCardSource {
    fun findCards(player: ServerPlayer): List<ItemStack>
}

@LazyInternalApi
public object ConfigurationCardSources {
    private val externalSources = CopyOnWriteArrayList<ConfigurationCardSource>()

    fun register(source: ConfigurationCardSource) {
        externalSources += source
    }

    /** Every card the player carries. Order is not significant; callers that need the held card read it directly. */
    fun allCards(player: ServerPlayer): List<ItemStack> =
        buildList {
            add(player.offhandItem)
            addAll(player.inventory.items)
            externalSources.forEach { source -> addAll(source.findCards(player)) }
        }.filter(ConfigurationCardRegistries::isCard)

    /** Blank cards never participate: they carry no intent and would otherwise make every pair ambiguous. */
    internal fun selectConfiguration(player: ServerPlayer): ConfigurationCardSelection {
        val configurations =
            allCards(player)
                .map(ConfigurationCardData::get)
                .filterNot(IoConfiguration::isDefault)
                .distinct()
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

/**
 * Copies a carried card onto a freshly placed machine.
 *
 * A dropped machine never carries its settings, so this is the only way a machine starts out
 * configured; the guard still holds for the rare machine that somehow arrives non-default.
 */
internal fun Level.applyConfigurationCardOnPlacement(
    pos: BlockPos,
    placer: Player?,
) {
    if (isClientSide) return
    val serverPlayer = placer as? ServerPlayer ?: return
    val blockEntity = getBlockEntity(pos) as? IoManagedBlockEntity ?: return
    if (!blockEntity.storedIoConfiguration().isDefault) return
    when (val selection = ConfigurationCardSources.selectConfiguration(serverPlayer)) {
        is ConfigurationCardSelection.Selected ->
            if (!blockEntity.ioController.applyConfiguration(selection.configuration)) {
                serverPlayer.displayActionBar("message.lazy.configuration_card.incompatible")
            }
        ConfigurationCardSelection.Missing -> Unit
        ConfigurationCardSelection.Ambiguous -> serverPlayer.displayActionBar("message.lazy.configuration_card.ambiguous")
    }
}

/**
 * Gives a held configuration card priority over a machine's own screen.
 *
 * IO blocks call this first in `useItemOn`; a non-null result skips their normal handling so the
 * card's [ConfigurationCardItem.useOn] runs instead.
 */
internal fun ioCardInteraction(stack: ItemStack): ItemInteractionResult? =
    if (ConfigurationCardRegistries.isCard(stack)) ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION else null
