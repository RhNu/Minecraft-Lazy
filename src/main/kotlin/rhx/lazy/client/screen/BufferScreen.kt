package rhx.lazy.client.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.neoforged.neoforge.client.ClientTooltipFlag
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.network.PacketDistributor
import rhx.lazy.block.entity.BufferBlockEntity
import rhx.lazy.menu.BufferMenu
import rhx.lazy.network.ClearBufferPayload
import java.util.Optional
import kotlin.math.roundToInt

internal class BufferScreen(
    menu: BufferMenu,
    playerInventory: Inventory,
    title: Component,
) : AbstractContainerScreen<BufferMenu>(menu, playerInventory, title) {
    init {
        imageWidth = PANEL_WIDTH
        imageHeight = PANEL_HEIGHT
        inventoryLabelY = imageHeight + 1_000
    }

    private var clearButton: Button? = null

    override fun init() {
        super.init()
        clearButton =
            addRenderableWidget(
                Button
                    .builder(Component.translatable("gui.lazy.buffer.clear")) {
                        PacketDistributor.sendToServer(ClearBufferPayload(menu.containerId))
                    }.bounds(
                        leftPos + (PANEL_WIDTH - CLEAR_BUTTON_WIDTH) / 2,
                        topPos + CLEAR_BUTTON_Y,
                        CLEAR_BUTTON_WIDTH,
                        20,
                    ).build()
                    .also { it.active = menu.snapshot.hasContents() },
            )
    }

    override fun containerTick() {
        super.containerTick()
        clearButton?.active = menu.snapshot.hasContents()
    }

    override fun render(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
        renderBufferTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderBg(
        guiGraphics: GuiGraphics,
        partialTick: Float,
        mouseX: Int,
        mouseY: Int,
    ) {
        guiGraphics.fill(0, 0, width, height, SCREEN_SHADE)
        guiGraphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + PANEL_HEIGHT, PANEL_OUTER)
        guiGraphics.fill(leftPos + 1, topPos + 1, leftPos + PANEL_WIDTH - 1, topPos + PANEL_HEIGHT - 1, PANEL_INNER)

        repeat(BufferBlockEntity.ITEM_SLOT_COUNT) { slot ->
            renderItem(guiGraphics, slot)
        }
        repeat(BufferBlockEntity.FLUID_TANK_COUNT) { tank ->
            renderFluidTank(guiGraphics, tank)
        }
    }

    override fun renderLabels(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
    ) {
        guiGraphics.drawCenteredString(font, title, imageWidth / 2, 8, TEXT_COLOR)
    }

    private fun renderItem(
        graphics: GuiGraphics,
        slot: Int,
    ) {
        val x = leftPos + ITEM_START_X + slot * ITEM_SPACING
        val y = topPos + ITEM_Y
        graphics.fill(x, y, x + 24, y + 24, SLOT_BORDER)
        graphics.fill(x + 1, y + 1, x + 23, y + 23, SLOT_BACKGROUND)

        val item = menu.snapshot.items[slot]
        if (!item.template.isEmpty) {
            graphics.renderFakeItem(item.template, x + 4, y + 4)
        }
        graphics.drawCenteredString(
            font,
            item.count.toString(),
            x + ITEM_COMPONENT_WIDTH / 2,
            y + 29,
            TEXT_COLOR,
        )
    }

    private fun renderFluidTank(
        graphics: GuiGraphics,
        tank: Int,
    ) {
        val x = leftPos + FLUID_START_X + (tank % 2) * FLUID_COLUMN_SPACING
        val y = topPos + FLUID_START_Y + (tank / 2) * FLUID_ROW_SPACING
        val fluid = menu.snapshot.fluids[tank]
        val name =
            if (fluid.isEmpty) {
                Component.translatable("gui.lazy.buffer.empty").string
            } else {
                fluid.hoverName.string
            }
        graphics.drawCenteredString(
            font,
            font.plainSubstrByWidth(name, FLUID_COMPONENT_WIDTH - 4),
            x + FLUID_COMPONENT_WIDTH / 2,
            y,
            TEXT_COLOR,
        )

        graphics.fill(x, y + 11, x + FLUID_COMPONENT_WIDTH, y + 25, SLOT_BORDER)
        graphics.fill(x + 1, y + 12, x + FLUID_COMPONENT_WIDTH - 1, y + 24, SLOT_BACKGROUND)
        if (!fluid.isEmpty) {
            renderFluid(graphics, fluid, x, y)
        }
        graphics.drawCenteredString(
            font,
            Component.translatable("gui.lazy.buffer.fluid_amount", fluid.amount),
            x + FLUID_COMPONENT_WIDTH / 2,
            y + 29,
            TEXT_COLOR,
        )
    }

    private fun renderFluid(
        graphics: GuiGraphics,
        fluid: FluidStack,
        x: Int,
        y: Int,
    ) {
        val extension = IClientFluidTypeExtensions.of(fluid.fluid)
        val texture = extension.getStillTexture(fluid) ?: return
        val sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture)
        val fillWidth =
            ((FLUID_COMPONENT_WIDTH - 4f) * fluid.amount / BufferBlockEntity.FLUID_TANK_CAPACITY)
                .roundToInt()
                .coerceIn(1, FLUID_COMPONENT_WIDTH - 4)
        val tint = extension.getTintColor(fluid)
        graphics.setColor(
            ((tint shr 16) and 0xFF) / 255f,
            ((tint shr 8) and 0xFF) / 255f,
            (tint and 0xFF) / 255f,
            ((tint ushr 24) and 0xFF) / 255f,
        )
        graphics.blit(x + 2, y + 13, 0, fillWidth, 10, sprite)
        graphics.setColor(1f, 1f, 1f, 1f)
    }

    private fun renderBufferTooltip(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
    ) {
        repeat(BufferBlockEntity.ITEM_SLOT_COUNT) { slot ->
            val hovering =
                isHovering(
                    ITEM_START_X + slot * ITEM_SPACING,
                    ITEM_Y,
                    ITEM_COMPONENT_WIDTH,
                    ITEM_COMPONENT_HEIGHT,
                    mouseX.toDouble(),
                    mouseY.toDouble(),
                )
            if (hovering && renderItemTooltip(guiGraphics, slot, mouseX, mouseY)) {
                return
            }
        }
        repeat(BufferBlockEntity.FLUID_TANK_COUNT) { tank ->
            val hovering =
                isHovering(
                    FLUID_START_X + (tank % 2) * FLUID_COLUMN_SPACING,
                    FLUID_START_Y + (tank / 2) * FLUID_ROW_SPACING,
                    FLUID_COMPONENT_WIDTH,
                    FLUID_COMPONENT_HEIGHT,
                    mouseX.toDouble(),
                    mouseY.toDouble(),
                )
            if (hovering && renderFluidTooltip(guiGraphics, tank, mouseX, mouseY)) {
                return
            }
        }
    }

    private fun renderItemTooltip(
        guiGraphics: GuiGraphics,
        slot: Int,
        mouseX: Int,
        mouseY: Int,
    ): Boolean {
        val item = menu.snapshot.items[slot]
        if (item.template.isEmpty) return false
        val minecraft = Minecraft.getInstance()
        val defaultFlag =
            if (minecraft.options.advancedItemTooltips) {
                TooltipFlag.Default.ADVANCED
            } else {
                TooltipFlag.Default.NORMAL
            }
        val lines =
            item.template
                .getTooltipLines(
                    Item.TooltipContext.of(minecraft.level),
                    minecraft.player,
                    ClientTooltipFlag.of(defaultFlag),
                ).toMutableList()
        lines += Component.translatable("gui.lazy.buffer.item_amount", item.count)
        guiGraphics.renderTooltip(
            minecraft.font,
            lines,
            Optional.empty(),
            item.template,
            mouseX,
            mouseY,
        )
        return true
    }

    private fun renderFluidTooltip(
        guiGraphics: GuiGraphics,
        tank: Int,
        mouseX: Int,
        mouseY: Int,
    ): Boolean {
        val fluid = menu.snapshot.fluids[tank]
        if (fluid.isEmpty) return false
        guiGraphics.renderTooltip(
            font,
            listOf(
                fluid.hoverName,
                Component.translatable("gui.lazy.buffer.fluid_amount", fluid.amount),
            ),
            Optional.empty(),
            ItemStack.EMPTY,
            mouseX,
            mouseY,
        )
        return true
    }

    companion object {
        private const val PANEL_WIDTH = 224
        private const val PANEL_HEIGHT = 194
        private const val CLEAR_BUTTON_WIDTH = 96
        private const val CLEAR_BUTTON_Y = 166

        private const val ITEM_START_X = 8
        private const val ITEM_Y = 27
        private const val ITEM_SPACING = 27
        private const val ITEM_COMPONENT_WIDTH = 25
        private const val ITEM_COMPONENT_HEIGHT = 46

        private const val FLUID_START_X = 8
        private const val FLUID_START_Y = 76
        private const val FLUID_COLUMN_SPACING = 106
        private const val FLUID_ROW_SPACING = 44
        private const val FLUID_COMPONENT_WIDTH = 102
        private const val FLUID_COMPONENT_HEIGHT = 40

        private const val SCREEN_SHADE = 0xA0101010.toInt()
        private const val PANEL_OUTER = 0xE0101010.toInt()
        private const val PANEL_INNER = 0xFF2A2A2A.toInt()
        private const val SLOT_BORDER = 0xFF8B8B8B.toInt()
        private const val SLOT_BACKGROUND = 0xFF1A1A1A.toInt()
        private const val TEXT_COLOR = 0xFFE0E0E0.toInt()
    }
}
