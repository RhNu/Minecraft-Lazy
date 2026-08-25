package rhx.lazy.feature.voidworld

import net.minecraft.server.level.ServerLevel
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent
import net.neoforged.neoforge.common.world.chunk.TicketController
import rhx.lazy.core.lazyId

internal object EncapsulatedSpaceChunkLoading {
    private val controller = TicketController(lazyId("encapsulated_spaces"))

    fun register(event: RegisterTicketControllersEvent) {
        event.register(controller)
    }

    fun force(
        level: ServerLevel,
        space: EncapsulatedSpace,
    ) {
        setForced(level, space, true)
    }

    fun unforce(
        level: ServerLevel,
        space: EncapsulatedSpace,
    ) {
        setForced(level, space, false)
    }

    private fun setForced(
        level: ServerLevel,
        space: EncapsulatedSpace,
        forced: Boolean,
    ) {
        controller.forceChunk(
            level,
            space.id,
            space.anchorChunk.x,
            space.anchorChunk.z,
            forced,
            true,
        )
    }
}
