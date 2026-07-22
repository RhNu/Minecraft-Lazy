package rhx.compose.client

import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import rhx.compose.MOD_ID
import rhx.compose.client.screen.BufferScreen
import rhx.compose.registry.ModMenus

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
internal object ClientModEvents {
    @SubscribeEvent
    fun registerMenuScreens(event: RegisterMenuScreensEvent) {
        event.register(ModMenus.buffer.get(), ::BufferScreen)
    }
}
