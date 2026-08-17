package rhx.lazy.feature.simulation

import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

internal class DataModelItem(
    properties: Properties,
) : Item(properties) {
    override fun interactLivingEntity(
        stack: ItemStack,
        player: Player,
        interactionTarget: LivingEntity,
        usedHand: InteractionHand,
    ): InteractionResult {
        val entityTypeComponent = SimulationRegistries.entityTypeComponent.get()
        if (stack.has(entityTypeComponent)) {
            if (!player.isShiftKeyDown) return InteractionResult.PASS
            clearBinding(stack, player)
            return InteractionResult.sidedSuccess(player.level().isClientSide)
        }
        if (interactionTarget is Player || !EntitySimulationTargets.isAllowed(interactionTarget.type)) {
            return InteractionResult.PASS
        }
        if (!player.level().isClientSide) {
            val id = BuiltInRegistries.ENTITY_TYPE.getKey(interactionTarget.type)
            stack.set(entityTypeComponent, id)
            player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS,
                0.8f,
                1.6f,
            )
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide)
    }

    override fun use(
        level: Level,
        player: Player,
        usedHand: InteractionHand,
    ): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (!player.isShiftKeyDown || entityTypeId(stack) == null) return super.use(level, player, usedHand)
        clearBinding(stack, player)
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    override fun isFoil(stack: ItemStack): Boolean = entityTypeId(stack) != null || super.isFoil(stack)

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        val entityId = entityTypeId(stack)
        val entityType = entityId?.let(BuiltInRegistries.ENTITY_TYPE::getOptional)?.orElse(null)
        tooltipComponents +=
            if (entityType == null) {
                Component.translatable("tooltip.lazy.data_model.blank").withStyle(ChatFormatting.GRAY)
            } else {
                Component
                    .translatable("tooltip.lazy.data_model.bound", entityType.description)
                    .withStyle(ChatFormatting.AQUA)
            }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }

    companion object {
        fun entityTypeId(stack: ItemStack): ResourceLocation? =
            stack
                .takeIf { it.`is`(SimulationRegistries.dataModelItem.get()) }
                ?.get(SimulationRegistries.entityTypeComponent.get())

        fun boundTo(entityId: ResourceLocation): ItemStack =
            ItemStack(SimulationRegistries.dataModelItem.get()).apply {
                set(SimulationRegistries.entityTypeComponent.get(), entityId)
            }

        private fun clearBinding(
            stack: ItemStack,
            player: Player,
        ) {
            if (player.level().isClientSide) return
            stack.remove(SimulationRegistries.entityTypeComponent.get())
            player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.NOTE_BLOCK_HAT.value(),
                SoundSource.PLAYERS,
                0.8f,
                0.8f,
            )
        }
    }
}

internal object DataModelInteractionHandler {
    fun onEntityInteract(event: PlayerInteractEvent.EntityInteract) {
        val target = event.target as? LivingEntity ?: return
        val item = event.itemStack.item as? DataModelItem ?: return
        val result = item.interactLivingEntity(event.itemStack, event.entity, target, event.hand)
        if (result == InteractionResult.PASS) return
        event.cancellationResult = result
        event.isCanceled = true
    }
}
