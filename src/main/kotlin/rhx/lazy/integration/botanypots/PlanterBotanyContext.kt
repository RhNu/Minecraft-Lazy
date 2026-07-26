package rhx.lazy.integration.botanypots

import net.darkhax.botanypots.common.api.context.BotanyPotContext
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil
import net.darkhax.botanypots.common.impl.BotanyPotsMod
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.pattern.BlockInWorld
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3

internal class PlanterBotanyContext(
    private val planter: PlanterBlockEntity,
) : BotanyPotContext {
    override fun getItem(slotId: Int): ItemStack =
        when (slotId) {
            BOTANY_SOIL_SLOT -> getSoilItem()
            BOTANY_SEED_SLOT -> getSeedItem()
            BOTANY_TOOL_SLOT -> ItemStack.EMPTY
            else -> ItemStack.EMPTY
        }

    override fun size(): Int = BOTANY_CONTEXT_SIZE

    override fun getSoilItem(): ItemStack = planter.getInput(PlanterBlockEntity.SOIL_SLOT)

    override fun getSeedItem(): ItemStack = planter.getInput(PlanterBlockEntity.SEED_SLOT)

    override fun getHarvestItem(): ItemStack = ItemStack.EMPTY

    override fun createLootParams(state: BlockState?): LootParams {
        val level =
            planter.level as? ServerLevel
                ?: throw IllegalStateException("Loot parameters are only available on the server")
        val harvestTool =
            BotanyPotsMod.CONFIG
                .get()
                .gameplay.default_harvest_tool
                .apply(level) ?: ItemStack.EMPTY
        return LootParams
            .Builder(level)
            .withParameter(LootContextParams.BLOCK_STATE, state ?: planter.blockState)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(planter.blockPos))
            .withParameter(LootContextParams.TOOL, harvestTool)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, planter)
            .create(LootContextParamSets.BLOCK)
    }

    override fun runFunction(functionId: ResourceLocation) {
        planter.runFunction(functionId)
    }

    override fun getPlayer(): Player? = null

    override fun getInteractionItem(): ItemStack = ItemStack.EMPTY

    override fun getRequiredGrowthTicks(): Int = planter.requiredGrowthTicks()

    override fun isServerThread(): Boolean = planter.level?.isClientSide == false

    override fun getCrop(): Crop? = planter.activeCrop

    override fun getSoil(): Soil? = planter.activeSoil

    fun insertedPotBlockState(): BlockState? {
        val stack = planter.getInput(PlanterBlockEntity.POT_SLOT)
        val blockItem = stack.item as? BlockItem ?: return null
        val defaultState = blockItem.block.defaultBlockState()
        return stack.get(DataComponents.BLOCK_STATE)?.apply(defaultState) ?: defaultState
    }

    fun insertedPotInWorld(state: BlockState): BlockInWorld {
        val level =
            planter.level
                ?: throw IllegalStateException("Planter must have a level before matching pot predicates")
        return InsertedPotInWorld(level, planter.blockPos, state)
    }

    private class InsertedPotInWorld(
        level: net.minecraft.world.level.LevelReader,
        pos: net.minecraft.core.BlockPos,
        private val insertedState: BlockState,
    ) : BlockInWorld(level, pos, false) {
        override fun getState(): BlockState = insertedState

        override fun getEntity(): BlockEntity? = null
    }

    private companion object {
        const val BOTANY_SOIL_SLOT = 0
        const val BOTANY_SEED_SLOT = 1
        const val BOTANY_TOOL_SLOT = 2
        const val BOTANY_CONTEXT_SIZE = 3
    }
}
