package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import rhx.lazy.integration.api.LazyInternalApi

/**
 * One kind of tool the chamber understands. A module decides which stacks it reacts to and what
 * they contribute; nothing about the chamber itself knows what a weapon or an incinerator is.
 */
@LazyInternalApi
public interface SimulationToolModule {
    /** Stacks this module reacts to. Also decides what the tool slots accept at all. */
    public fun claims(stack: ItemStack): Boolean

    public fun contribute(
        stack: ItemStack,
        builder: SimulationToolLoadout.Builder,
    )
}

/**
 * The combined effect of everything sitting in the tool slots, rebuilt whenever a batch advances.
 *
 * Effects come in two shapes: single valued ones like the kill weapon, where the first tool slot
 * that offers one wins, and output transformations, which run in priority order. Equal-priority
 * transformations retain the order of their tool slots.
 */
@LazyInternalApi
public class SimulationToolLoadout private constructor(
    public val weapon: ItemStack,
    private val itemProcessors: List<ItemProcessor>,
) {
    internal fun processOutput(
        level: ServerLevel,
        stack: ItemStack,
    ): List<ItemStack> =
        itemProcessors.fold(listOf(stack)) { outputs, processor ->
            outputs.flatMap { output -> processor.processor(level, output) }
        }

    internal fun processorPriorities(): List<Int> = itemProcessors.map(ItemProcessor::priority)

    @LazyInternalApi
    public class Builder {
        private var weapon: ItemStack = ItemStack.EMPTY
        private val itemProcessors = mutableListOf<ItemProcessor>()
        private var processorOrder = 0

        /** First one wins: later tool slots holding a weapon are inert rather than overriding. */
        public fun weapon(stack: ItemStack) {
            if (weapon.isEmpty) weapon = stack
        }

        /**
         * Adds an output transformation. Lower priorities run first regardless of tool-slot order.
         * A transformation may return an empty list to destroy an output.
         */
        public fun processOutputs(
            priority: Int = 0,
            processor: (ServerLevel, ItemStack) -> List<ItemStack>,
        ) {
            itemProcessors += ItemProcessor(priority, processorOrder++, processor)
        }

        internal fun build(): SimulationToolLoadout =
            if (weapon.isEmpty && itemProcessors.isEmpty()) {
                EMPTY
            } else {
                SimulationToolLoadout(weapon, itemProcessors.sortedWith(compareBy(ItemProcessor::priority, ItemProcessor::order)))
            }
    }

    companion object {
        val EMPTY = SimulationToolLoadout(ItemStack.EMPTY, emptyList())
    }

    private data class ItemProcessor(
        val priority: Int,
        val order: Int,
        val processor: (ServerLevel, ItemStack) -> List<ItemStack>,
    )
}

@LazyInternalApi
public object SimulationToolModules {
    private val modules = linkedMapOf<ResourceLocation, SimulationToolModule>()

    init {
        register(WeaponToolModule.ID, WeaponToolModule)
        register(FurnaceToolModule.ID, FurnaceToolModule)
        register(BlastFurnaceToolModule.ID, BlastFurnaceToolModule)
        register(SmokerToolModule.ID, SmokerToolModule)
        register(IncineratorToolModule.ID, IncineratorToolModule)
        register(GrindstoneToolModule.ID, GrindstoneToolModule)
    }

    @Synchronized
    public fun register(
        id: ResourceLocation,
        module: SimulationToolModule,
    ) {
        require(modules.putIfAbsent(id, module) == null) { "Duplicate simulation tool module $id" }
    }

    public fun claims(stack: ItemStack): Boolean = !stack.isEmpty && snapshot().any { module -> module.claims(stack) }

    /** Slot order is precedence order, so the loadout is built by walking the slots front to back. */
    public fun loadout(tools: List<ItemStack>): SimulationToolLoadout =
        if (tools.all(ItemStack::isEmpty)) SimulationToolLoadout.EMPTY else buildLoadout(snapshot(), tools)

    internal fun buildLoadout(
        modules: Collection<SimulationToolModule>,
        tools: List<ItemStack>,
    ): SimulationToolLoadout {
        val builder = SimulationToolLoadout.Builder()
        tools.forEach { stack ->
            if (stack.isEmpty) return@forEach
            modules.forEach { module -> if (module.claims(stack)) module.contribute(stack, builder) }
        }
        return builder.build()
    }

    private fun snapshot(): Collection<SimulationToolModule> = synchronized(this) { modules.values.toList() }
}
