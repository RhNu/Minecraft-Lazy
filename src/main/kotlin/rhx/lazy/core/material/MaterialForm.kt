package rhx.lazy.core.material

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.item.Item

/**
 * One common shape a material takes: the `c:` tag prefix that names it, and how much material a
 * single piece is worth.
 *
 * Units count sixteenths of a nugget — 144 to the ingot — which is the molten-volume convention
 * Mekanism and GregTech already share. Picking that denominator is what keeps the table free of
 * fractions: a rod is half an ingot (72) and a gear is four (576), both whole numbers, so every
 * conversion stays in integer arithmetic and nothing is ever rounded away.
 *
 * [prefix] is a tag prefix rather than a whole tag, because the material name is the part that
 * varies: `c:nuggets/` plus `iron` is the iron nugget tag. It is a prefix and not a
 * `<root>/<material>` template because raw storage blocks are spelled `c:storage_blocks/raw_iron`,
 * not `c:raw_storage_blocks/iron`, and a prefix covers both shapes without a special case.
 */
internal data class MaterialForm(
    val prefix: String,
    val units: Int,
) {
    val tagNamespace: String
    val pathPrefix: String

    init {
        require(units > 0) { "A material form must be worth a positive number of units: $prefix" }
        val separator = prefix.indexOf(':')
        require(separator > 0 && separator < prefix.length - 1) {
            "A material form prefix must look like <namespace>:<path prefix>: $prefix"
        }
        tagNamespace = prefix.substring(0, separator)
        pathPrefix = prefix.substring(separator + 1)
        require(ResourceLocation.isValidNamespace(tagNamespace) && ResourceLocation.isValidPath(pathPrefix)) {
            "A material form prefix must be a valid resource location prefix: $prefix"
        }
    }

    /** The material [tag] names under this form, or null when the tag belongs to another form. */
    fun materialOf(tag: ResourceLocation): String? {
        if (tag.namespace != tagNamespace || !tag.path.startsWith(pathPrefix)) return null
        return tag.path.substring(pathPrefix.length).takeIf(String::isNotEmpty)
    }

    /** Null when [material] would not spell a valid resource location, which a datapack can cause. */
    fun tagFor(material: String): TagKey<Item>? {
        val path = pathPrefix + material
        if (!ResourceLocation.isValidPath(path)) return null
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(tagNamespace, path))
    }

    companion object {
        private val PREFIX_CODEC: Codec<String> =
            Codec.STRING.comapFlatMap(
                { value ->
                    val separator = value.indexOf(':')
                    when {
                        separator <= 0 || separator >= value.length - 1 ->
                            DataResult.error { "A material form prefix must look like <namespace>:<path prefix>: $value" }
                        !ResourceLocation.isValidNamespace(value.substring(0, separator)) ->
                            DataResult.error { "Invalid namespace in material form prefix: $value" }
                        !ResourceLocation.isValidPath(value.substring(separator + 1)) ->
                            DataResult.error { "Invalid path in material form prefix: $value" }
                        else -> DataResult.success(value)
                    }
                },
                { it },
            )

        val CODEC: Codec<MaterialForm> =
            RecordCodecBuilder.create { instance ->
                instance
                    .group(
                        PREFIX_CODEC.fieldOf("prefix").forGetter(MaterialForm::prefix),
                        ExtraCodecs.POSITIVE_INT.fieldOf("units").forGetter(MaterialForm::units),
                    ).apply(instance, ::MaterialForm)
            }
    }
}
