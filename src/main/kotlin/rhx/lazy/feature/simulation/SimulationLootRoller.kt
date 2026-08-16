package rhx.lazy.feature.simulation

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.neoforged.neoforge.common.util.FakePlayerFactory
import rhx.lazy.mixin.LivingEntityAccessor

internal object SimulationLootRoller {
    /**
     * Rolls one kill worth of loot.
     *
     * [weapon] is the stack a tool slot offered as the murder weapon. Vanilla reads the killer's
     * main hand for looting, for equipment drop chances and for weapon predicates, so handing the
     * fake player a copy is the whole integration. The copy also means nothing wears out: the
     * chamber never runs an attack, so no durability is ever spent.
     */
    fun roll(
        level: ServerLevel,
        entity: LivingEntity,
        override: ResourceLocation?,
        weapon: ItemStack,
        output: (ItemStack) -> Unit,
    ) {
        val player = FakePlayerFactory.getMinecraft(level)
        if (!weapon.isEmpty) player.setItemInHand(InteractionHand.MAIN_HAND, weapon.copy())
        entity.tickCount = PLAYER_KILL_TIME
        entity.setLastHurtByPlayer(player)
        val damageSource = level.damageSources().playerAttack(player)
        val captured = mutableListOf<ItemEntity>()
        entity.captureDrops(captured)
        try {
            // The generic "this weapon just hit it" pipeline, so fire aspect cooks drops without a case of its own.
            if (!weapon.isEmpty) EnchantmentHelper.doPostAttackEffects(level, entity, damageSource)
            val params =
                LootParams
                    .Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, entity)
                    .withParameter(LootContextParams.ORIGIN, entity.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player)
                    .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                    .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                    .withLuck(player.luck)
                    .create(LootContextParamSets.ENTITY)
            val key = override?.let { ResourceKey.create(Registries.LOOT_TABLE, it) } ?: entity.lootTable
            level.server
                .reloadableRegistries()
                .getLootTable(key)
                .getRandomItems(params)
                .filterNot(ItemStack::isEmpty)
                .forEach(output)
            val accessor = entity as LivingEntityAccessor
            accessor.`lazy$dropCustomDeathLoot`(level, damageSource, true)
            accessor.`lazy$dropEquipment`()
        } finally {
            entity.captureDrops(null)
            // The fake player is shared for the whole level, so it never keeps the weapon past this roll.
            if (!weapon.isEmpty) player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
        }
        captured.map(ItemEntity::getItem).filterNot(ItemStack::isEmpty).forEach(output)
    }

    private const val PLAYER_KILL_TIME = 100
}
