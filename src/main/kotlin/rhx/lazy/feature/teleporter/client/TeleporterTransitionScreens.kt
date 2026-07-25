package rhx.lazy.feature.teleporter.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ReceivingLevelScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent
import rhx.lazy.MOD_ID
import rhx.lazy.feature.voidworld.VoidWorldKeys
import java.util.function.BooleanSupplier

@EventBusSubscriber(modid = MOD_ID, value = [Dist.CLIENT])
internal object TeleporterTransitionScreens {
    @SubscribeEvent
    fun register(event: RegisterDimensionTransitionScreenEvent) {
        event.registerIncomingEffect(VoidWorldKeys.voidLevel) { levelReady, reason ->
            TeleporterTransitionScreen(
                levelReady,
                reason,
                Component.translatable(TRAVELING_TO_VOID),
            )
        }
        event.registerOutgoingEffect(VoidWorldKeys.voidLevel) { levelReady, reason ->
            TeleporterTransitionScreen(
                levelReady,
                reason,
                returningMessage(),
            )
        }
    }

    private fun returningMessage(): Component {
        val targetDimension = Minecraft.getInstance().level?.dimension()
        return Component.translatable(
            RETURNING,
            targetDimension?.displayName() ?: Component.literal("?"),
        )
    }

    private fun ResourceKey<Level>.displayName(): Component {
        val id = location()
        return Component.translatableWithFallback(id.toLanguageKey("dimension"), id.toString())
    }

    private const val RETURNING = "screen.lazy.teleporter.returning"
    private const val TRAVELING_TO_VOID = "screen.lazy.teleporter.traveling_to_void"
}

private class TeleporterTransitionScreen(
    levelReady: BooleanSupplier,
    reason: Reason,
    private val message: Component,
) : ReceivingLevelScreen(StableLevelReady(levelReady), reason) {
    override fun render(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, message, width / 2, height / 2 - 50, TEXT_COLOR)
    }

    override fun getNarrationMessage(): Component = message

    private companion object {
        const val TEXT_COLOR = 0xFFFFFF
    }
}

private class StableLevelReady(
    private val vanillaReady: BooleanSupplier,
) : BooleanSupplier {
    private var stableTicks = 0

    override fun getAsBoolean(): Boolean {
        val level = Minecraft.getInstance().level
        val ready =
            vanillaReady.asBoolean &&
                level != null &&
                level.isLightUpdateQueueEmpty
        stableTicks = if (ready) stableTicks + 1 else 0
        return stableTicks >= REQUIRED_STABLE_TICKS
    }

    private companion object {
        const val REQUIRED_STABLE_TICKS = 2
    }
}
