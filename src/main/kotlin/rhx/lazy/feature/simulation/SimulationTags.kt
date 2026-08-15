package rhx.lazy.feature.simulation

import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import rhx.lazy.core.lazyId

internal object SimulationTags {
    val automaticPlantTargets: TagKey<Item> =
        TagKey.create(Registries.ITEM, lazyId("automatic_plant"))
    val automaticSimulationBlacklist: TagKey<Item> =
        TagKey.create(Registries.ITEM, lazyId("automatic_simulation_blacklist"))
    val automaticTreeBlacklist: TagKey<Item> =
        TagKey.create(Registries.ITEM, lazyId("automatic_tree_blacklist"))
    val automaticCropBlacklist: TagKey<Item> =
        TagKey.create(Registries.ITEM, lazyId("automatic_crop_blacklist"))
    val automaticPlantBlacklist: TagKey<Item> =
        TagKey.create(Registries.ITEM, lazyId("automatic_plant_blacklist"))
    val automaticMineralBlacklist: TagKey<Item> =
        TagKey.create(Registries.ITEM, lazyId("automatic_mineral_blacklist"))
    val automaticMysticalBlacklist: TagKey<Item> =
        TagKey.create(Registries.ITEM, lazyId("automatic_mystical_blacklist"))
    val dataModelBlacklist: TagKey<EntityType<*>> =
        TagKey.create(Registries.ENTITY_TYPE, lazyId("data_model_blacklist"))

    fun automaticBlacklist(source: net.minecraft.resources.ResourceLocation): TagKey<Item>? =
        when (source.path) {
            "tree" -> automaticTreeBlacklist
            "crop" -> automaticCropBlacklist
            "plant" -> automaticPlantBlacklist
            "mineral" -> automaticMineralBlacklist
            "mystical" -> automaticMysticalBlacklist
            else -> null
        }
}
