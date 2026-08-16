package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

/**
 * One kind of tool the chamber understands. A module decides which stacks it reacts to and what
 * they contribute; nothing about the chamber itself knows what a weapon or an incinerator is.
 */
internal interface SimulationToolModule {
    /** Stacks this module reacts to. Also decides what the tool slots accept at all. */
    fun claims(stack: ItemStack): Boolean

    fun contribute(
        stack: ItemStack,
        builder: SimulationToolLoadout.Builder,
    )
}

/**
 * The combined effect of everything sitting in the tool slots, rebuilt whenever a batch advances.
 *
 * Effects come in two shapes: single valued ones like the kill weapon, where the first tool slot
 * that offers one wins, and additive ones like output filters, which simply stack.
 */
internal class SimulationToolLoadout private constructor(
    val weapon: ItemStack,
    private val rejects: List<(ItemStack) -> Boolean>,
) {
    fun acceptsOutput(stack: ItemStack): Boolean = rejects.none { reject -> reject(stack) }

    class Builder {
        private var weapon: ItemStack = ItemStack.EMPTY
        private val rejects = mutableListOf<(ItemStack) -> Boolean>()

        /** First one wins: later tool slots holding a weapon are inert rather than overriding. */
        fun weapon(stack: ItemStack) {
            if (weapon.isEmpty) weapon = stack
        }

        fun rejectOutputs(filter: (ItemStack) -> Boolean) {
            rejects += filter
        }

        fun build(): SimulationToolLoadout =
            if (weapon.isEmpty && rejects.isEmpty()) EMPTY else SimulationToolLoadout(weapon, rejects.toList())
    }

    companion object {
        val EMPTY = SimulationToolLoadout(ItemStack.EMPTY, emptyList())
    }
}

internal object SimulationToolModules {
    private val modules = linkedMapOf<ResourceLocation, SimulationToolModule>()

    init {
        register(WeaponToolModule.ID, WeaponToolModule)
        register(IncineratorToolModule.ID, IncineratorToolModule)
    }

    @Synchronized
    fun register(
        id: ResourceLocation,
        module: SimulationToolModule,
    ) {
        require(modules.putIfAbsent(id, module) == null) { "Duplicate simulation tool module $id" }
    }

    fun claims(stack: ItemStack): Boolean = !stack.isEmpty && snapshot().any { module -> module.claims(stack) }

    /** Slot order is precedence order, so the loadout is built by walking the slots front to back. */
    fun loadout(tools: List<ItemStack>): SimulationToolLoadout =
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
