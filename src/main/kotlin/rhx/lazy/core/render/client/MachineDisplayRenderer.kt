package rhx.lazy.core.render.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.Level
import rhx.lazy.core.MachineBlock
import rhx.lazy.core.MachineBlockEntity
import rhx.lazy.core.render.MachineActivity

/**
 * Draws a machine's display icon on its front face, as if the panel were a screen showing a sample.
 *
 * The front is the face the player pointed the machine at when they placed it, and the same face the
 * IO panel calls front, so the readable side is always the side they chose. Putting the icon there
 * instead of on top is what lets machines be stacked and walled without hiding each other.
 *
 * Any machine can use this: register it for the block entity type and override
 * [MachineBlockEntity.computeDisplayState]. Nothing here knows about a specific machine.
 */
internal class MachineDisplayRenderer<T : MachineBlockEntity>(
    context: BlockEntityRendererProvider.Context,
) : BlockEntityRenderer<T> {
    private val itemRenderer: ItemRenderer = context.itemRenderer

    override fun render(
        blockEntity: T,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
    ) {
        val state = blockEntity.displayState() ?: return
        if (state.isEmpty) return
        val level = blockEntity.level ?: return
        val facing = blockEntity.blockState.getOptionalValue(MachineBlock.FACING).orElse(null) ?: return
        val front = blockEntity.blockPos.relative(facing)
        if (level.getBlockState(front).isSolidRender(level, front)) return

        poseStack.pushPose()
        poseStack.translate(
            BLOCK_CENTER + facing.stepX * ICON_DISTANCE,
            BLOCK_CENTER,
            BLOCK_CENTER + facing.stepZ * ICON_DISTANCE,
        )
        // The GUI transform leaves an item's front on +Z, which is south; turn that onto the facing.
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()))
        poseStack.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE)
        itemRenderer.renderStatic(
            state.icon,
            // GUI is the only context that sizes a flat item and a block model alike, so a seed and
            // a moss block read as the same kind of icon instead of one being twice the other.
            ItemDisplayContext.GUI,
            iconLight(level, front, state.activity),
            iconOverlay(state.activity),
            poseStack,
            bufferSource,
            level,
            blockEntity.blockPos.asLong().hashCode(),
        )
        poseStack.popPose()
    }

    override fun getViewDistance(): Int = VIEW_DISTANCE

    /**
     * A working machine lights its own screen, an idle one is left to the room. Across a wall of
     * machines that difference is what separates the ones that are doing something from the rest.
     */
    private fun iconLight(
        level: Level,
        front: BlockPos,
        activity: MachineActivity,
    ): Int =
        when (activity) {
            MachineActivity.IDLE -> LevelRenderer.getLightColor(level, front)
            MachineActivity.RUNNING, MachineActivity.BLOCKED -> LightTexture.FULL_BRIGHT
        }

    /**
     * Blocked machines borrow the damage tint entities use. It survives any room lighting, which the
     * lit/unlit difference on its own does not.
     */
    private fun iconOverlay(activity: MachineActivity): Int =
        if (activity == MachineActivity.BLOCKED) BLOCKED_OVERLAY else OverlayTexture.NO_OVERLAY

    private companion object {
        val BLOCKED_OVERLAY = OverlayTexture.pack(OverlayTexture.NO_WHITE_U, OverlayTexture.RED_OVERLAY_V)

        const val VIEW_DISTANCE = 24
        const val BLOCK_CENTER = 0.5

        /**
         * Centre to icon along the facing. The floor is the overlay quad the machine model puts a
         * hundredth of a model unit — 0.0006 blocks — outside the face; a flat icon is about 0.014
         * deep at [ICON_SCALE], so this leaves its back a sixtieth of a block clear of the panel art
         * and reads as printed on the screen rather than hovering in front of it. Block-model icons
         * are isometric and sit half sunk into the panel, which is what keeps them close too: the
         * buried half is behind their own front faces and never visible.
         */
        const val ICON_DISTANCE = 0.53

        /** The bezel window is eight pixels wide; this leaves the icon a pixel of margin inside it. */
        const val ICON_SCALE = 0.45f
    }
}
