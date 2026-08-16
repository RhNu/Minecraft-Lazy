package rhx.lazy.feature.simulation

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import rhx.lazy.MOD_ID
import rhx.lazy.core.lazyId

internal object SimulationTags {
    val plantTargets: TagKey<Item> = itemTag("simulation/target/plant")
    val duplicateSelfTargets: TagKey<Item> = itemTag("simulation/target/duplicate_self")
    val weaponTools: TagKey<Item> = itemTag("simulation/tool/weapon")
    val incineratorTools: TagKey<Item> = itemTag("simulation/tool/incinerator")
    val incineratedOutputs: TagKey<Item> = itemTag("simulation/incinerated")
    val blacklist: TagKey<Item> = itemTag("simulation/blacklist/all")
    val dataModelBlacklist: TagKey<EntityType<*>> =
        TagKey.create(Registries.ENTITY_TYPE, lazyId("simulation/blacklist/data_model"))

    /** `lazy:tree` becomes `lazy:simulation/blacklist/tree`; other namespaces keep their own segment. */
    fun sourceBlacklist(source: ResourceLocation): TagKey<Item> =
        if (source.namespace == MOD_ID) {
            itemTag("simulation/blacklist/${source.path}")
        } else {
            itemTag("simulation/blacklist/${source.namespace}/${source.path}")
        }

    private fun itemTag(path: String): TagKey<Item> = TagKey.create(Registries.ITEM, lazyId(path))
}
