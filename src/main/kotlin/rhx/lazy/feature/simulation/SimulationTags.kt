package rhx.lazy.feature.simulation

import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import rhx.lazy.core.lazyId

internal object SimulationTags {
    val automaticMineralBlacklist: TagKey<Item> =
        TagKey.create(Registries.ITEM, lazyId("automatic_mineral_blacklist"))
    val dataModelBlacklist: TagKey<EntityType<*>> =
        TagKey.create(Registries.ENTITY_TYPE, lazyId("data_model_blacklist"))
}
