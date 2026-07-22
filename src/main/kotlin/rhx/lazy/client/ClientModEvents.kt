package rhx.lazy.client

import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import rhx.lazy.MOD_ID
import rhx.lazy.client.screen.BufferScreen
import rhx.lazy.registry.ModMenus

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
internal object ClientModEvents {
    @SubscribeEvent
    fun registerMenuScreens(event: RegisterMenuScreensEvent) {
        event.register(ModMenus.buffer.get(), ::BufferScreen)
    }
}
