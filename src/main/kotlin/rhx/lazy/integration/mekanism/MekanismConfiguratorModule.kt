package rhx.lazy.integration.mekanism

import mekanism.api.IConfigCardAccess
import mekanism.api.SerializationConstants
import mekanism.api.Upgrade
import mekanism.api.security.IBlockSecurityUtils
import mekanism.common.capabilities.Capabilities
import mekanism.common.item.interfaces.IUpgradeItem
import mekanism.common.tile.interfaces.IUpgradeTile
import mekanism.common.util.WorldUtils
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import rhx.lazy.core.configurator.ModularConfiguratorDataAccess
import rhx.lazy.core.configurator.ModularConfiguratorInventory
import rhx.lazy.core.configurator.ModularConfiguratorModule
import rhx.lazy.core.lazyId

internal object MekanismConfiguratorModule : ModularConfiguratorModule {
    override val id: ResourceLocation = lazyId("mekanism")

    override fun acceptsMaterial(stack: ItemStack): Boolean = stack.item is IUpgradeItem

    override fun useOn(context: UseOnContext): InteractionResult? {
        val player = context.player ?: return null
        val level = context.level
        val access =
            WorldUtils.getCapability(
                level,
                Capabilities.CONFIG_CARD,
                context.clickedPos,
                context.clickedFace,
            ) ?: return null

        if (!IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, level, context.clickedPos)) {
            if (!level.isClientSide) display(player, "message.lazy.modular_configurator.mekanism.security_failed")
            return InteractionResult.FAIL
        }

        val configurator = context.itemInHand
        if (player.isShiftKeyDown) {
            if (!level.isClientSide) copyConfiguration(context, access, configurator)
            return InteractionResult.sidedSuccess(level.isClientSide)
        }

        val payload = ModularConfiguratorDataAccess.modulePayload(configurator, id)
        if (payload == null) {
            if (!level.isClientSide) display(player, "message.lazy.modular_configurator.mekanism.nothing_to_paste")
            return InteractionResult.sidedSuccess(level.isClientSide)
        }
        val configuration = payload.compoundOrNull(CONFIGURATION_TAG)
        val storedType = payload.getString(MACHINE_TYPE_TAG).takeIf(String::isNotEmpty)
        if (configuration == null || storedType == null) {
            if (!level.isClientSide) display(player, "message.lazy.modular_configurator.mekanism.invalid_data")
            return InteractionResult.FAIL
        }

        val actualType = BuiltInRegistries.BLOCK.getKey(access.configurationDataType).toString()
        if (storedType != actualType) {
            if (!level.isClientSide) {
                display(
                    player,
                    "message.lazy.modular_configurator.mekanism.machine_mismatch",
                    storedMachineName(configuration),
                    Component.translatable(access.configCardName),
                )
            }
            return InteractionResult.FAIL
        }

        if (!level.isClientSide) pasteConfiguration(context, access, configurator, configuration, payload)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    private fun copyConfiguration(
        context: UseOnContext,
        access: IConfigCardAccess,
        configurator: ItemStack,
    ) {
        val player = requireNotNull(context.player)
        val level = context.level
        try {
            val configuration = access.getConfigurationData(level.registryAccess(), player).copy()
            configuration.putString(SerializationConstants.DATA_NAME, access.configCardName)
            val machineType = BuiltInRegistries.BLOCK.getKey(access.configurationDataType)
            configuration.putString(SerializationConstants.DATA_TYPE, machineType.toString())
            val upgrades = installedUpgrades(access)
            val payload =
                CompoundTag().apply {
                    putString(MACHINE_TYPE_TAG, machineType.toString())
                    put(CONFIGURATION_TAG, configuration)
                    if (upgrades.isNotEmpty()) {
                        put(UPGRADES_TAG, CompoundTag().also { tag -> Upgrade.saveMap(upgrades, tag) })
                    }
                }
            ModularConfiguratorDataAccess.setModulePayload(configurator, id, payload)
            if (upgrades.isEmpty()) {
                display(
                    player,
                    "message.lazy.modular_configurator.mekanism.copied_no_upgrades",
                    Component.translatable(access.configCardName),
                )
            } else {
                display(
                    player,
                    "message.lazy.modular_configurator.mekanism.copied",
                    Component.translatable(access.configCardName),
                    upgradeSummary(upgrades),
                )
            }
        } catch (_: RuntimeException) {
            display(player, "message.lazy.modular_configurator.mekanism.copy_failed")
        }
    }

    private fun pasteConfiguration(
        context: UseOnContext,
        access: IConfigCardAccess,
        configurator: ItemStack,
        configuration: CompoundTag,
        payload: CompoundTag,
    ) {
        val player = requireNotNull(context.player)
        try {
            access.setConfigurationData(context.level.registryAccess(), player, configuration.copy())
            access.configurationDataSet()
        } catch (_: RuntimeException) {
            display(player, "message.lazy.modular_configurator.mekanism.apply_failed")
            return
        }

        val desired = payload.compoundOrNull(UPGRADES_TAG)?.let(Upgrade::buildMap).orEmpty()
        if (desired.isEmpty()) {
            display(player, "message.lazy.modular_configurator.mekanism.applied_no_upgrades")
            return
        }

        val upgradeTile = access as? IUpgradeTile
        val current =
            if (upgradeTile?.supportsUpgrades() == true) {
                Upgrade.entries.associateWith { upgrade -> upgradeTile.component.getUpgrades(upgrade) }
            } else {
                emptyMap()
            }
        val plan = UpgradePastePlan.create(Upgrade.entries, desired, current)
        if (plan.required.isEmpty()) {
            display(player, "message.lazy.modular_configurator.mekanism.applied_satisfied")
            return
        }

        val installed =
            if (upgradeTile?.supportsUpgrades() == true) {
                installUpgrades(configurator, upgradeTile, plan.required)
            } else {
                emptyMap()
            }
        val missing = plan.missing(installed)
        when (plan.status(installed)) {
            UpgradePasteStatus.COMPLETE ->
                display(
                    player,
                    "message.lazy.modular_configurator.mekanism.applied_complete",
                    upgradeSummary(installed),
                )
            UpgradePasteStatus.PARTIAL ->
                display(
                    player,
                    "message.lazy.modular_configurator.mekanism.applied_partial",
                    upgradeSummary(installed),
                    upgradeSummary(missing),
                )
            UpgradePasteStatus.NONE_INSTALLED ->
                display(
                    player,
                    "message.lazy.modular_configurator.mekanism.applied_none",
                    upgradeSummary(missing),
                )
            UpgradePasteStatus.ALREADY_SATISFIED ->
                display(player, "message.lazy.modular_configurator.mekanism.applied_satisfied")
        }
    }

    private fun installedUpgrades(access: IConfigCardAccess): Map<Upgrade, Int> {
        val upgradeTile = access as? IUpgradeTile ?: return emptyMap()
        if (!upgradeTile.supportsUpgrades()) return emptyMap()
        return buildMap {
            Upgrade.entries.forEach { upgrade ->
                val count = upgradeTile.component.getUpgrades(upgrade)
                if (count > 0) put(upgrade, count)
            }
        }
    }

    private fun installUpgrades(
        configurator: ItemStack,
        upgradeTile: IUpgradeTile,
        required: Map<Upgrade, Int>,
    ): Map<Upgrade, Int> {
        val inventory = ModularConfiguratorInventory(configurator)
        val installed = linkedMapOf<Upgrade, Int>()
        Upgrade.entries.forEach { upgrade ->
            val needed = required.getOrDefault(upgrade, 0)
            if (needed <= 0 || !upgradeTile.component.supports(upgrade)) return@forEach
            val available = available(inventory, upgrade).coerceAtMost(needed)
            if (available <= 0) return@forEach
            val added = upgradeTile.component.addUpgrades(upgrade, available)
            if (added > 0) {
                consume(inventory, upgrade, added)
                installed[upgrade] = added
            }
        }
        return installed
    }

    private fun available(
        inventory: ModularConfiguratorInventory,
        upgrade: Upgrade,
    ): Int =
        (0 until inventory.slots).sumOf { slot ->
            val stack = inventory.getStackInSlot(slot)
            if (stack.upgradeTypeOrNull() == upgrade) stack.count else 0
        }

    private fun consume(
        inventory: ModularConfiguratorInventory,
        upgrade: Upgrade,
        amount: Int,
    ) {
        var remaining = amount
        for (slot in 0 until inventory.slots) {
            if (remaining <= 0) break
            val stack = inventory.getStackInSlot(slot)
            if (stack.upgradeTypeOrNull() != upgrade) continue
            val extracted = inventory.extractItem(slot, remaining.coerceAtMost(stack.count), false)
            remaining -= extracted.count
        }
        check(remaining == 0) { "Configurator upgrade inventory changed while installing $upgrade" }
    }

    private fun ItemStack.upgradeTypeOrNull(): Upgrade? = (item as? IUpgradeItem)?.getUpgradeType(this)

    private fun upgradeSummary(upgrades: Map<Upgrade, Int>): Component =
        ComponentUtils.formatList(
            Upgrade.entries.filter { upgrade -> upgrades.getOrDefault(upgrade, 0) > 0 },
        ) { upgrade ->
            Component.translatable(
                "message.lazy.modular_configurator.mekanism.upgrade_entry",
                Component.translatable(upgrade.translationKey),
                upgrades.getValue(upgrade),
            )
        }

    private fun storedMachineName(configuration: CompoundTag): Component =
        configuration
            .getString(SerializationConstants.DATA_NAME)
            .takeIf(String::isNotEmpty)
            ?.let(Component::translatable)
            ?: Component.translatable("message.lazy.modular_configurator.mekanism.unknown_machine")

    private fun CompoundTag.compoundOrNull(key: String): CompoundTag? = takeIf { contains(key, Tag.TAG_COMPOUND.toInt()) }?.getCompound(key)

    private fun display(
        player: net.minecraft.world.entity.player.Player,
        key: String,
        vararg arguments: Any,
    ) {
        player.displayClientMessage(Component.translatable(key, *arguments), true)
    }

    private const val MACHINE_TYPE_TAG = "machine_type"
    private const val CONFIGURATION_TAG = "configuration"
    private const val UPGRADES_TAG = "upgrades"
}
