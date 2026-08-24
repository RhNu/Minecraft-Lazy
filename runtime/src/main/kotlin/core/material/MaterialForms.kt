package rhx.lazy.core.material

import net.minecraft.core.Registry
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.neoforged.neoforge.registries.DataPackRegistryEvent
import rhx.lazy.core.lazyId
import rhx.lazy.integration.api.LazyInternalApi

/**
 * The material form table, as a synced datapack registry.
 *
 * A datapack registry rather than a hardcoded list because packs have to be able to reach it: only
 * seven of these tag groups exist in NeoForge itself, the rest arrive with whichever mods are
 * installed, and a pack that adds `c:sheets/` or disagrees about what a gear costs needs somewhere
 * to say so. Entries live in `data/<pack>/lazy/material_form/<name>.json`.
 *
 * Registering it with a network codec means clients receive the same table during the configuration
 * phase, so the recipe preview a player sees is built from exactly the values the server converts
 * with.
 */
@LazyInternalApi
public object MaterialForms {
    val REGISTRY_KEY: ResourceKey<Registry<MaterialForm>> = ResourceKey.createRegistryKey(lazyId("material_form"))

    /** The denominator every other value is expressed in. */
    const val INGOT_UNITS = 144

    val NUGGET = key("nugget")
    val INGOT = key("ingot")
    val GEM = key("gem")
    val DUST = key("dust")
    val RAW_MATERIAL = key("raw_material")
    val PLATE = key("plate")
    val ROD = key("rod")
    val WIRE = key("wire")
    val GEAR = key("gear")
    val STORAGE_BLOCK = key("storage_block")
    val RAW_STORAGE_BLOCK = key("raw_storage_block")

    /**
     * Shipped defaults. `c:storage_blocks/raw_` deliberately overlaps `c:storage_blocks/`; the index
     * resolves that by preferring the longer literal prefix, so `c:storage_blocks/raw_iron` is read
     * as iron's raw block rather than as a block of some material called `raw_iron`.
     */
    private val DEFAULTS: Map<ResourceKey<MaterialForm>, MaterialForm> =
        linkedMapOf(
            NUGGET to MaterialForm("c:nuggets/", INGOT_UNITS / 9),
            INGOT to MaterialForm("c:ingots/", INGOT_UNITS),
            GEM to MaterialForm("c:gems/", INGOT_UNITS),
            DUST to MaterialForm("c:dusts/", INGOT_UNITS),
            RAW_MATERIAL to MaterialForm("c:raw_materials/", INGOT_UNITS),
            PLATE to MaterialForm("c:plates/", INGOT_UNITS),
            ROD to MaterialForm("c:rods/", INGOT_UNITS / 2),
            WIRE to MaterialForm("c:wires/", INGOT_UNITS / 2),
            GEAR to MaterialForm("c:gears/", INGOT_UNITS * 4),
            STORAGE_BLOCK to MaterialForm("c:storage_blocks/", INGOT_UNITS * 9),
            RAW_STORAGE_BLOCK to MaterialForm("c:storage_blocks/raw_", INGOT_UNITS * 9),
        )

    fun bootstrap(context: BootstrapContext<MaterialForm>) {
        DEFAULTS.forEach { (key, form) -> context.register(key, form) }
    }

    fun registerDataPackRegistry(event: DataPackRegistryEvent.NewRegistry) {
        event.dataPackRegistry(REGISTRY_KEY, MaterialForm.CODEC, MaterialForm.CODEC)
    }

    /** `lazy:plate` becomes `material_form.lazy.plate`; other namespaces keep their own segment. */
    fun translationKey(form: ResourceKey<MaterialForm>): String =
        "material_form.${form.location().namespace}.${form.location().path.replace('/', '.')}"

    private fun key(path: String): ResourceKey<MaterialForm> = ResourceKey.create(REGISTRY_KEY, lazyId(path))
}
