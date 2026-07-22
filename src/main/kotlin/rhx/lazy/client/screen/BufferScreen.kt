package rhx.lazy.client.screen

import com.daqem.uilib.api.client.gui.IRenderable
import com.daqem.uilib.client.gui.AbstractContainerScreen
import com.daqem.uilib.client.gui.component.AbstractComponent
import com.daqem.uilib.client.gui.component.SolidColorComponent
import com.daqem.uilib.client.gui.component.TextComponent
import com.daqem.uilib.client.gui.component.io.ButtonComponent
import com.daqem.uilib.client.gui.text.Text
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
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

    private var clearButton: ButtonComponent? = null

    override fun startScreen() {
        setBackground(null)
        setPauseScreen(false)
        addComponent(SolidColorComponent(0, 0, width, height, SCREEN_SHADE))

        val panel = SolidColorComponent(0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_OUTER)
        panel.center()
        panel.addChild(SolidColorComponent(1, 1, PANEL_WIDTH - 2, PANEL_HEIGHT - 2, PANEL_INNER))

        val titleText = Text(font, title)
        val titleComponent = TextComponent(titleText)
        titleComponent.setX((PANEL_WIDTH - titleComponent.width) / 2)
        titleComponent.setY(8)
        panel.addChild(titleComponent)

        repeat(BufferBlockEntity.ITEM_SLOT_COUNT) { slot ->
            panel.addChild(BufferItemComponent(8 + slot * 27, 27, slot, menu))
        }
        repeat(BufferBlockEntity.FLUID_TANK_COUNT) { tank ->
            panel.addChild(
                BufferFluidComponent(
                    8 + (tank % 2) * 106,
                    76 + (tank / 2) * 44,
                    tank,
                    menu,
                ),
            )
        }
        clearButton =
            ButtonComponent(
                (PANEL_WIDTH - CLEAR_BUTTON_WIDTH) / 2,
                166,
                CLEAR_BUTTON_WIDTH,
                20,
                Component.translatable("gui.lazy.buffer.clear"),
            ) { _, _, _, _, mouseButton ->
                if (mouseButton != 0) {
                    false
                } else {
                    PacketDistributor.sendToServer(ClearBufferPayload(menu.containerId))
                    true
                }
            }.also { button ->
                button.setEnabled(menu.snapshot.hasContents())
                panel.addChild(button)
            }
        addComponent(panel)
    }

    override fun onTickScreen(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        clearButton?.setEnabled(menu.snapshot.hasContents())
    }

    override fun onResizeScreenRepositionComponents(
        width: Int,
        height: Int,
    ) {
        components.clear()
        startScreen()
        components.forEach(IRenderable<*>::startRenderable)
    }

    override fun renderLabels(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
    ) = Unit

    private class BufferItemComponent(
        x: Int,
        y: Int,
        private val slot: Int,
        private val menu: BufferMenu,
    ) : AbstractComponent<BufferItemComponent>(null, x, y, ITEM_COMPONENT_WIDTH, ITEM_COMPONENT_HEIGHT) {
        override fun render(
            graphics: GuiGraphics,
            mouseX: Int,
            mouseY: Int,
            delta: Float,
        ) {
            graphics.fill(0, 0, 24, 24, SLOT_BORDER)
            graphics.fill(1, 1, 23, 23, SLOT_BACKGROUND)

            val item = menu.snapshot.items[slot]
            if (!item.template.isEmpty) {
                graphics.renderFakeItem(item.template, 4, 4)
            }
            graphics.drawCenteredString(
                Minecraft.getInstance().font,
                item.count.toString(),
                width / 2,
                29,
                TEXT_COLOR,
            )
        }

        override fun renderTooltips(
            guiGraphics: GuiGraphics,
            mouseX: Int,
            mouseY: Int,
            delta: Float,
        ) {
            val item = menu.snapshot.items[slot]
            if (item.template.isEmpty || !isTotalHovered(mouseX.toDouble(), mouseY.toDouble())) return
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
            lines +=
                Component.translatable(
                    "gui.lazy.buffer.item_amount",
                    item.count,
                )
            guiGraphics.renderTooltip(
                minecraft.font,
                lines,
                Optional.empty(),
                item.template,
                mouseX,
                mouseY,
            )
        }
    }

    private class BufferFluidComponent(
        x: Int,
        y: Int,
        private val tank: Int,
        private val menu: BufferMenu,
    ) : AbstractComponent<BufferFluidComponent>(null, x, y, FLUID_COMPONENT_WIDTH, FLUID_COMPONENT_HEIGHT) {
        override fun render(
            graphics: GuiGraphics,
            mouseX: Int,
            mouseY: Int,
            delta: Float,
        ) {
            val fluid = menu.snapshot.fluids[tank]
            val font = Minecraft.getInstance().font
            val name =
                if (fluid.isEmpty) {
                    Component.translatable("gui.lazy.buffer.empty").string
                } else {
                    fluid.hoverName.string
                }
            graphics.drawCenteredString(
                font,
                font.plainSubstrByWidth(name, width - 4),
                width / 2,
                0,
                TEXT_COLOR,
            )

            graphics.fill(0, 11, width, 25, SLOT_BORDER)
            graphics.fill(1, 12, width - 1, 24, SLOT_BACKGROUND)
            if (!fluid.isEmpty) {
                renderFluid(graphics, fluid)
            }
            graphics.drawCenteredString(
                font,
                Component.translatable(
                    "gui.lazy.buffer.fluid_amount",
                    fluid.amount,
                ),
                width / 2,
                29,
                TEXT_COLOR,
            )
        }

        private fun renderFluid(
            graphics: GuiGraphics,
            fluid: FluidStack,
        ) {
            val extension = IClientFluidTypeExtensions.of(fluid.fluid)
            val texture = extension.getStillTexture(fluid) ?: return
            val sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture)
            val fillWidth =
                ((width - 4f) * fluid.amount / BufferBlockEntity.FLUID_TANK_CAPACITY)
                    .roundToInt()
                    .coerceIn(1, width - 4)
            val tint = extension.getTintColor(fluid)
            graphics.setColor(
                ((tint shr 16) and 0xFF) / 255f,
                ((tint shr 8) and 0xFF) / 255f,
                (tint and 0xFF) / 255f,
                ((tint ushr 24) and 0xFF) / 255f,
            )
            graphics.blit(2, 13, 0, fillWidth, 10, sprite)
            graphics.setColor(1f, 1f, 1f, 1f)
        }

        override fun renderTooltips(
            guiGraphics: GuiGraphics,
            mouseX: Int,
            mouseY: Int,
            delta: Float,
        ) {
            val fluid = menu.snapshot.fluids[tank]
            if (fluid.isEmpty || !isTotalHovered(mouseX.toDouble(), mouseY.toDouble())) return
            guiGraphics.renderTooltip(
                Minecraft.getInstance().font,
                listOf(
                    fluid.hoverName,
                    Component.translatable(
                        "gui.lazy.buffer.fluid_amount",
                        fluid.amount,
                    ),
                ),
                Optional.empty(),
                ItemStack.EMPTY,
                mouseX,
                mouseY,
            )
        }
    }

    companion object {
        private const val PANEL_WIDTH = 224
        private const val PANEL_HEIGHT = 194
        private const val CLEAR_BUTTON_WIDTH = 96
        private const val ITEM_COMPONENT_WIDTH = 25
        private const val ITEM_COMPONENT_HEIGHT = 46
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
