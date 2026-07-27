package rhx.lazy.integration.botanypots.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemDisplayContext
import rhx.lazy.integration.botanypots.PlanterBlock
import rhx.lazy.integration.botanypots.PlanterBlockEntity

internal class PlanterBlockEntityRenderer(
    context: BlockEntityRendererProvider.Context,
) : BlockEntityRenderer<PlanterBlockEntity> {
    private val itemRenderer: ItemRenderer = context.itemRenderer

    override fun render(
        blockEntity: PlanterBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
    ) {
        val level = blockEntity.level ?: return
        val seed = blockEntity.seedForRendering
        if (seed.isEmpty) return

        val facing = blockEntity.blockState.getValue(PlanterBlock.FACING)
        poseStack.pushPose()
        poseStack.translate(BLOCK_CENTER, TOP_SURFACE, BLOCK_CENTER)
        poseStack.mulPose(Axis.YP.rotationDegrees(BASE_NORTH_YAW - facing.toYRot()))
        poseStack.mulPose(Axis.XP.rotationDegrees(LAY_FLAT_PITCH))
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE)
        val surfaceLight = LevelRenderer.getLightColor(level, blockEntity.blockPos.above())
        itemRenderer.renderStatic(
            seed,
            ItemDisplayContext.FIXED,
            surfaceLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            level,
            blockEntity.blockPos.asLong().hashCode(),
        )
        poseStack.popPose()
    }

    override fun shouldRenderOffScreen(blockEntity: PlanterBlockEntity): Boolean = false

    override fun getViewDistance(): Int = VIEW_DISTANCE

    private companion object {
        const val VIEW_DISTANCE = 24
        const val BLOCK_CENTER = 0.5
        const val TOP_SURFACE = 1.002
        const val BASE_NORTH_YAW = 180f

        // FIXED turns generated item fronts toward -Z; +90 degrees leaves that front facing upward.
        const val LAY_FLAT_PITCH = 90f
        const val ITEM_SCALE = 0.625f
    }
}
