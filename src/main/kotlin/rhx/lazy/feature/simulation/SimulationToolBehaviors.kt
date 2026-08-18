package rhx.lazy.feature.simulation

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import rhx.lazy.core.lazyId

/**
 * A weapon in a tool slot means "kill the simulated entity with this". The stack itself is never
 * touched, so nothing wears out; the chamber only hands a copy to the fake killer.
 */
internal object WeaponToolModule : SimulationToolModule {
    val ID: ResourceLocation = lazyId("weapon")

    override fun claims(stack: ItemStack): Boolean = stack.`is`(SimulationTags.weaponTools)

    override fun contribute(
        stack: ItemStack,
        builder: SimulationToolLoadout.Builder,
    ) = builder.weapon(stack)
}

/** A lava bucket melts every piece of gear the simulation would otherwise hand out. */
internal object IncineratorToolModule : SimulationToolModule {
    val ID: ResourceLocation = lazyId("incinerator")

    override fun claims(stack: ItemStack): Boolean = stack.`is`(SimulationTags.incineratorTools)

    override fun contribute(
        stack: ItemStack,
        builder: SimulationToolLoadout.Builder,
    ) = builder.rejectOutputs { output -> output.`is`(SimulationTags.incineratedOutputs) }
}

/** A dispenser lets ordinary IO modes settle the entire completed batch in the same tick. */
internal object DispenserToolModule : SimulationToolModule {
    val ID: ResourceLocation = lazyId("dispenser")

    override fun claims(stack: ItemStack): Boolean = stack.`is`(SimulationTags.dispenserTools)

    override fun contribute(
        stack: ItemStack,
        builder: SimulationToolLoadout.Builder,
    ) = builder.settleBatchImmediately()
}
