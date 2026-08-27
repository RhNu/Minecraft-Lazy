package rhx.lazy.feature.simulation

import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID
import rhx.lazy.integration.api.LazyInternalApi

/** A targeted compatibility hook for death drops that are implemented outside loot tables. */
@LazyInternalApi
public fun interface SimulationLivingDropsHandler {
    public fun onLivingDrops(event: LivingDropsEvent)
}

internal class SimulationLivingDropsHandlerRegistry {
    private val logger = LogManager.getLogger("$MOD_ID/SimulationLivingDropsHandlers")
    private val handlers = mutableListOf<SimulationLivingDropsHandler>()

    fun register(handler: SimulationLivingDropsHandler) {
        check(handlers.none { registered -> registered === handler }) {
            "Simulation living drops handler is already registered: ${handler.javaClass.name}"
        }
        handlers += handler
    }

    fun dispatch(
        entity: LivingEntity,
        damageSource: DamageSource,
        drops: MutableCollection<ItemEntity>,
    ) {
        if (handlers.isEmpty()) return
        val event = LivingDropsEvent(entity, damageSource, drops, true)
        handlers.forEach { handler ->
            try {
                handler.onLivingDrops(event)
            } catch (error: LinkageError) {
                logger.error("Simulation living drops handler linkage failed for {}", entity.encodeId, error)
            } catch (exception: RuntimeException) {
                logger.error("Simulation living drops handler failed for {}", entity.encodeId, exception)
            }
        }
    }
}

@LazyInternalApi
public object SimulationLivingDropsHandlers {
    private val registry = SimulationLivingDropsHandlerRegistry()

    public fun register(handler: SimulationLivingDropsHandler) {
        registry.register(handler)
    }

    internal fun dispatch(
        entity: LivingEntity,
        damageSource: DamageSource,
        drops: MutableCollection<ItemEntity>,
    ) {
        registry.dispatch(entity, damageSource, drops)
    }
}
