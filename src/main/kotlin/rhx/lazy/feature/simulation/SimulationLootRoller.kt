package rhx.lazy.feature.simulation

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.neoforged.neoforge.common.util.FakePlayerFactory
import rhx.lazy.mixin.LivingEntityAccessor

internal object SimulationLootRoller {
    fun roll(
        level: ServerLevel,
        entity: LivingEntity,
        override: ResourceLocation?,
        output: (ItemStack) -> Unit,
    ) {
        val player = FakePlayerFactory.getMinecraft(level)
        entity.tickCount = PLAYER_KILL_TIME
        entity.setLastHurtByPlayer(player)
        val damageSource = level.damageSources().playerAttack(player)
        val captured = mutableListOf<ItemEntity>()
        entity.captureDrops(captured)
        try {
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
        }
        captured.map(ItemEntity::getItem).filterNot(ItemStack::isEmpty).forEach(output)
    }

    private const val PLAYER_KILL_TIME = 100
}
