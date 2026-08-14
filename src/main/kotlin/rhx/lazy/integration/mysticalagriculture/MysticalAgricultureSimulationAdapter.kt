package rhx.lazy.integration.mysticalagriculture

import com.blakebr0.mysticalagriculture.api.MysticalAgricultureAPI
import com.blakebr0.mysticalagriculture.api.crop.Crop
import com.blakebr0.mysticalagriculture.config.ModConfigs
import com.blakebr0.mysticalagriculture.init.ModBlocks
import com.blakebr0.mysticalagriculture.init.ModItems
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import rhx.lazy.core.lazyId
import rhx.lazy.feature.simulation.AutomaticSimulationAdapter
import rhx.lazy.feature.simulation.AutomaticSimulationAdapters
import rhx.lazy.feature.simulation.AutomaticSimulationCandidate
import rhx.lazy.feature.simulation.SimulationConfigs
import rhx.lazy.feature.simulation.SimulationItemOutput

internal object MysticalAgricultureSimulationAdapter : AutomaticSimulationAdapter {
    @Volatile
    private var cropIndex: Map<Item, Crop>? = null

    fun register() {
        AutomaticSimulationAdapters.register(SOURCE, this)
    }

    override fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate? {
        val crop = cropsBySeed()[stack.item]?.takeIf(Crop::isEnabled) ?: return null
        val secondaryChance = crop.getSecondaryChance(ModBlocks.INFERIUM_FARMLAND.get()).toFloat().coerceIn(0f, 1f)
        val outputs =
            buildList {
                add(SimulationItemOutput(ItemStack(crop.seedsItem)))
                add(SimulationItemOutput(ItemStack(crop.essenceItem)))
                if (ModConfigs.SECONDARY_SEED_DROPS.get() && secondaryChance > 0f) {
                    add(SimulationItemOutput(ItemStack(crop.seedsItem), secondaryChance))
                }
                if (secondaryChance > 0f) {
                    add(SimulationItemOutput(ItemStack(crop.essenceItem), secondaryChance))
                }
                val fertilizedChance =
                    ModConfigs.FERTILIZED_ESSENCE_DROP_CHANCE
                        .get()
                        .toFloat()
                        .coerceIn(0f, 1f)
                if (fertilizedChance > 0f) {
                    add(SimulationItemOutput(ItemStack(ModItems.FERTILIZED_ESSENCE.get()), fertilizedChance))
                }
            }
        val seedId = BuiltInRegistries.ITEM.getKey(stack.item)
        return AutomaticSimulationCandidate(
            SOURCE,
            lazyId("automatic/mystical/${seedId.namespace}/${seedId.path}"),
            SimulationConfigs.settings.defaultDuration.get(),
            PRIORITY,
            claimsInput = true,
            itemOutputs = outputs,
        )
    }

    override fun settingsFingerprint(): Any =
        MysticalSettings(
            ModConfigs.SECONDARY_SEED_DROPS.get(),
            ModConfigs.FERTILIZED_ESSENCE_DROP_CHANCE.get(),
            cropsBySeed()
                .values
                .filter(Crop::isEnabled)
                .map(Crop::getId)
                .sortedBy(ResourceLocation::toString),
        )

    private fun cropsBySeed(): Map<Item, Crop> =
        cropIndex ?: synchronized(this) {
            cropIndex ?: MysticalAgricultureAPI
                .getCropRegistry()
                .crops
                .associateBy { it.seedsItem as Item }
                .also { cropIndex = it }
        }

    private val SOURCE: ResourceLocation = lazyId("mystical")
    private const val PRIORITY = 300

    private data class MysticalSettings(
        val secondarySeedDrops: Boolean,
        val fertilizedEssenceChance: Double,
        val enabledCrops: List<ResourceLocation>,
    )
}
