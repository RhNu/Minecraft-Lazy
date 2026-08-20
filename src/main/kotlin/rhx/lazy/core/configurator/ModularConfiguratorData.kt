package rhx.lazy.core.configurator

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

internal class ModularConfiguratorMaterialEntry(
    val slot: Int,
    stack: ItemStack,
    val count: Int,
) {
    private val storedTemplate = stack.copyWithCount(1)

    val template: ItemStack
        get() = storedTemplate.copy()

    fun asStack(): ItemStack = storedTemplate.copyWithCount(count)

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is ModularConfiguratorMaterialEntry &&
            slot == other.slot &&
            count == other.count &&
            ItemStack.isSameItemSameComponents(storedTemplate, other.storedTemplate)

    override fun hashCode(): Int = 31 * (31 * slot + ItemStack.hashItemAndComponents(storedTemplate)) + count

    companion object {
        val CODEC: Codec<ModularConfiguratorMaterialEntry> =
            RecordCodecBuilder.create { builder ->
                builder
                    .group(
                        Codec.INT.fieldOf("slot").forGetter(ModularConfiguratorMaterialEntry::slot),
                        ItemStack.SINGLE_ITEM_CODEC
                            .fieldOf("stack")
                            .forGetter(ModularConfiguratorMaterialEntry::template),
                        Codec.INT.fieldOf("count").forGetter(ModularConfiguratorMaterialEntry::count),
                    ).apply(builder, ::ModularConfiguratorMaterialEntry)
            }
    }
}

internal class ModularConfiguratorData private constructor(
    private val materials: List<ModularConfiguratorMaterialEntry>,
    private val modulePayloads: Map<ResourceLocation, CompoundTag>,
) {
    fun stack(slot: Int): ItemStack = materials.firstOrNull { entry -> entry.slot == slot }?.asStack() ?: ItemStack.EMPTY

    fun withStack(
        slot: Int,
        stack: ItemStack,
    ): ModularConfiguratorData {
        require(slot in 0 until SLOT_COUNT) { "Invalid modular configurator slot $slot" }
        val retained = materials.filterNot { entry -> entry.slot == slot }.toMutableList()
        if (!stack.isEmpty && stack.count > 0) {
            retained += ModularConfiguratorMaterialEntry(slot, stack, stack.count.coerceAtMost(SLOT_LIMIT))
        }
        return create(retained, modulePayloads)
    }

    fun modulePayload(id: ResourceLocation): CompoundTag? = modulePayloads[id]?.copy()

    fun withModulePayload(
        id: ResourceLocation,
        payload: CompoundTag,
    ): ModularConfiguratorData = create(materials, modulePayloads + (id to payload.copy()))

    fun clearModulePayloads(): ModularConfiguratorData = if (modulePayloads.isEmpty()) this else create(materials, emptyMap())

    fun hasModulePayloads(): Boolean = modulePayloads.isNotEmpty()

    internal fun encodedMaterials(): List<ModularConfiguratorMaterialEntry> =
        materials.map { entry -> ModularConfiguratorMaterialEntry(entry.slot, entry.template, entry.count) }

    internal fun encodedModulePayloads(): Map<ResourceLocation, CompoundTag> = modulePayloads.mapValues { (_, payload) -> payload.copy() }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is ModularConfiguratorData &&
            materials == other.materials &&
            modulePayloads == other.modulePayloads

    override fun hashCode(): Int = 31 * materials.hashCode() + modulePayloads.hashCode()

    companion object {
        const val SLOT_COUNT = 18
        const val SLOT_LIMIT = 1024

        val EMPTY = ModularConfiguratorData(emptyList(), emptyMap())

        private val MODULE_PAYLOADS_CODEC: Codec<Map<ResourceLocation, CompoundTag>> =
            Codec.unboundedMap(ResourceLocation.CODEC, CompoundTag.CODEC)

        val CODEC: Codec<ModularConfiguratorData> =
            RecordCodecBuilder.create { builder ->
                builder
                    .group(
                        ModularConfiguratorMaterialEntry.CODEC
                            .listOf()
                            .optionalFieldOf("materials", emptyList())
                            .forGetter(ModularConfiguratorData::encodedMaterials),
                        MODULE_PAYLOADS_CODEC
                            .optionalFieldOf("modules", emptyMap())
                            .forGetter(ModularConfiguratorData::encodedModulePayloads),
                    ).apply(builder, ::create)
            }

        fun create(
            materials: List<ModularConfiguratorMaterialEntry>,
            modulePayloads: Map<ResourceLocation, CompoundTag>,
        ): ModularConfiguratorData {
            val seenSlots = mutableSetOf<Int>()
            val normalizedMaterials =
                materials
                    .asSequence()
                    .filter { entry ->
                        entry.slot in 0 until SLOT_COUNT &&
                            entry.count > 0 &&
                            !entry.template.isEmpty &&
                            seenSlots.add(entry.slot)
                    }.map { entry ->
                        ModularConfiguratorMaterialEntry(
                            entry.slot,
                            entry.template,
                            entry.count.coerceAtMost(SLOT_LIMIT),
                        )
                    }.sortedBy(ModularConfiguratorMaterialEntry::slot)
                    .toList()
            val copiedPayloads = modulePayloads.mapValues { (_, payload) -> payload.copy() }
            return if (normalizedMaterials.isEmpty() && copiedPayloads.isEmpty()) {
                EMPTY
            } else {
                ModularConfiguratorData(normalizedMaterials, copiedPayloads)
            }
        }
    }
}
