package rhx.lazy.core.ui.client

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import rhx.lazy.core.ui.LargeItemCountFormatter
import kotlin.math.min

/** Client-only rendering details kept out of the common slot and handler implementation. */
internal object LargeItemSlotRenderer {
    fun draw(
        context: GUIContext,
        stack: ItemStack,
        count: Long,
        formatter: LargeItemCountFormatter,
    ) {
        val displayStack = stack.copyWithCount(1)
        DrawerHelper.drawItemStack(
            context.graphics,
            displayStack,
            0,
            0,
            context.elementColor,
            null,
        )
        if (count <= 1L) return

        val text = formatter.format(count)
        if (text.isEmpty()) return
        val font =
            IClientItemExtensions
                .of(displayStack)
                .getFont(displayStack, IClientItemExtensions.FontContext.ITEM_COUNT)
                ?: context.mc.font
        val textWidth = font.width(text).coerceAtLeast(1)
        val scale = min(1f, MAX_TEXT_WIDTH / textWidth)
        val renderedWidth = textWidth * scale
        val renderedHeight = font.lineHeight * scale

        context.pose.pushPose()
        context.pose.translate(
            SLOT_SIZE - renderedWidth,
            SLOT_SIZE - renderedHeight,
            TEXT_Z,
        )
        context.pose.scale(scale, scale, 1f)
        context.graphics.drawString(font, text, 0, 0, TEXT_COLOR, true)
        context.pose.popPose()
    }

    private const val SLOT_SIZE = 16f
    private const val MAX_TEXT_WIDTH = 14f
    private const val TEXT_Z = 232f
    private const val TEXT_COLOR = 0xFFFFFF
}
