package rhx.lazy.feature.simulation

import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SingleRecipeInput
import net.minecraft.world.item.enchantment.ItemEnchantments
import rhx.lazy.core.lazyId
import rhx.lazy.integration.api.LazyInternalApi

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

/** A furnace applies a matching smelting recipe to each output batch. */
internal object FurnaceToolModule : SimulationToolModule {
    val ID: ResourceLocation = lazyId("furnace")

    override fun claims(stack: ItemStack): Boolean = stack.`is`(Items.FURNACE)

    override fun contribute(
        stack: ItemStack,
        builder: SimulationToolLoadout.Builder,
    ) = builder.processOutputs { level, output -> processCookingRecipe(level, RecipeType.SMELTING, output) }
}

/** A blast furnace applies a matching blasting recipe to each output batch. */
internal object BlastFurnaceToolModule : SimulationToolModule {
    val ID: ResourceLocation = lazyId("blast_furnace")

    override fun claims(stack: ItemStack): Boolean = stack.`is`(Items.BLAST_FURNACE)

    override fun contribute(
        stack: ItemStack,
        builder: SimulationToolLoadout.Builder,
    ) = builder.processOutputs { level, output -> processCookingRecipe(level, RecipeType.BLASTING, output) }
}

/** A smoker applies a matching smoking recipe to each output batch. */
internal object SmokerToolModule : SimulationToolModule {
    val ID: ResourceLocation = lazyId("smoker")

    override fun claims(stack: ItemStack): Boolean = stack.`is`(Items.SMOKER)

    override fun contribute(
        stack: ItemStack,
        builder: SimulationToolLoadout.Builder,
    ) = builder.processOutputs { level, output -> processCookingRecipe(level, RecipeType.SMOKING, output) }
}

/** Lava buckets and magma blocks melt eligible drops, with integrations able to recover special items first. */
internal object IncineratorToolModule : SimulationToolModule {
    val ID: ResourceLocation = lazyId("incinerator")

    override fun claims(stack: ItemStack): Boolean = stack.`is`(SimulationTags.incineratorTools)

    override fun contribute(
        stack: ItemStack,
        builder: SimulationToolLoadout.Builder,
    ) = builder.processOutputs(priority = INCINERATION_PRIORITY) { level, output ->
        if (!output.`is`(SimulationTags.incineratedOutputs) && !SimulationIncinerationHandlers.claims(output)) {
            listOf(output)
        } else {
            SimulationIncinerationHandlers.process(level, output) ?: emptyList()
        }
    }
}

/** A grindstone removes every enchanted drop after incineration has finished processing it. */
internal object GrindstoneToolModule : SimulationToolModule {
    val ID: ResourceLocation = lazyId("grindstone")

    override fun claims(stack: ItemStack): Boolean = stack.`is`(Items.GRINDSTONE)

    override fun contribute(
        stack: ItemStack,
        builder: SimulationToolLoadout.Builder,
    ) = builder.processOutputs(priority = GRINDSTONE_PRIORITY) { _, output ->
        if (output.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty) listOf(output) else emptyList()
    }
}

/**
 * Optional integrations recover outputs which the incinerator would otherwise destroy.
 * Handlers return a replacement list so an empty recipe result still counts as a handled item.
 */
@LazyInternalApi
public interface SimulationIncinerationHandler {
    public fun claims(stack: ItemStack): Boolean

    public fun process(
        level: ServerLevel,
        stack: ItemStack,
    ): List<ItemStack>
}

@LazyInternalApi
public object SimulationIncinerationHandlers {
    private val handlers = mutableListOf<SimulationIncinerationHandler>()

    @Synchronized
    public fun register(handler: SimulationIncinerationHandler) {
        require(handler !in handlers) { "Duplicate simulation incineration handler $handler" }
        handlers += handler
    }

    internal fun claims(stack: ItemStack): Boolean = snapshot().any { handler -> handler.claims(stack) }

    internal fun process(
        level: ServerLevel,
        stack: ItemStack,
    ): List<ItemStack>? {
        val handler = snapshot().firstOrNull { candidate -> candidate.claims(stack) } ?: return null
        return (1..stack.count).flatMap { handler.process(level, stack.copyWithCount(1)) }
    }

    private fun snapshot(): List<SimulationIncinerationHandler> = synchronized(this) { handlers.toList() }
}

private const val INCINERATION_PRIORITY = 0
private const val GRINDSTONE_PRIORITY = 100

private fun <T : AbstractCookingRecipe> processCookingRecipe(
    level: ServerLevel,
    recipeType: RecipeType<T>,
    output: ItemStack,
): List<ItemStack> {
    val input = SingleRecipeInput(output)
    val recipe = level.recipeManager.getRecipeFor(recipeType, input, level).orElse(null) ?: return listOf(output)
    val result = recipe.value().assemble(input, level.registryAccess())
    return processCookingResult(output, result)
}

internal fun processCookingResult(
    output: ItemStack,
    result: ItemStack,
): List<ItemStack> {
    if (result.isEmpty) return emptyList()
    val resultCount = Math.multiplyExact(output.count.toLong(), result.count.toLong())
    return listOf(result.copyWithCount(Math.toIntExact(resultCount)))
}
