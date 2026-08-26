package rhx.lazy.feature.simulation

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.Property
import rhx.lazy.LazyRuntime
import rhx.lazy.core.lazyId
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public data class RegistryPlantSimulationSpec(
    val id: String,
    val input: ResourceLocation,
    val parts: List<RegistryBlockLootPart>,
    val priority: Int = 400,
    val tools: List<SimulationToolRequirement> = emptyList(),
    val itemOutputs: List<RegistryItemOutputPart> = emptyList(),
) {
    init {
        require(parts.isNotEmpty() || itemOutputs.isNotEmpty()) {
            "Registry plant simulation $id must contain at least one output"
        }
        requireValidSimulationTools(tools)
    }
}

@LazyInternalApi
public data class RegistryItemOutputPart(
    val item: ResourceLocation,
    val count: Int = 1,
    val chance: Float = 1f,
    val minRolls: Int = 1,
    val maxRolls: Int = 1,
) {
    init {
        require(count > 0)
        require(chance in 0f..1f)
        require(minRolls >= 0 && maxRolls >= minRolls)
    }
}

@LazyInternalApi
public data class RegistryBlockLootPart(
    val block: ResourceLocation,
    val properties: Map<String, String> = emptyMap(),
    val tool: ResourceLocation? = null,
    val chance: Float = 1f,
    val minRolls: Int = 1,
    val maxRolls: Int = 1,
) {
    init {
        require(chance in 0f..1f)
        require(minRolls >= 0 && maxRolls >= minRolls)
    }
}

/** Registers registry-only adapters, keeping third-party classes outside Lazy's class loader. */
@LazyInternalApi
public fun registerRegistryPlantSimulations(
    integration: String,
    specs: List<RegistryPlantSimulationSpec>,
) {
    specs.forEach { spec ->
        val source = lazyId("plant_integration/$integration/${spec.id}")
        AutomaticSimulationAdapters.register(
            source,
            RegistryPlantAdapter(source, integration, spec),
        )
    }
}

private class RegistryPlantAdapter(
    private val source: ResourceLocation,
    private val integration: String,
    private val spec: RegistryPlantSimulationSpec,
) : AutomaticSimulationAdapter {
    private val loggedErrors = hashSetOf<String>()

    override fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate? {
        if (BuiltInRegistries.ITEM.getKey(stack.item) != spec.input) return null
        if (!validateTools()) return null
        val outputs = spec.parts.mapNotNull(::resolvePart)
        if (outputs.size != spec.parts.size) return null
        val itemOutputs = spec.itemOutputs.mapNotNull(::resolveItemOutput)
        if (itemOutputs.size != spec.itemOutputs.size) return null
        outputs.forEach { output ->
            if (SimulationLootDisplays.items(level, output.state, output.tool).isEmpty()) {
                report("empty mature loot for ${BuiltInRegistries.BLOCK.getKey(output.state.block)} state ${output.state.values}")
                return null
            }
        }
        return AutomaticSimulationCandidate(
            source,
            automaticId(source, spec.input.namespace, spec.input.path),
            SimulationConfigs.settings.defaultDuration.get(),
            spec.priority,
            claimsInput = true,
            itemOutputs = itemOutputs,
            blockLootOutputs = outputs,
            tools = spec.tools,
            group = lazyId("plant_integration/$integration"),
        )
    }

    override fun toolRequirements(): List<SimulationToolRequirement> = spec.tools

    private fun validateTools(): Boolean {
        spec.tools.filterIsInstance<SimulationToolRequirement.BlockTag>().forEach { requirement ->
            val values = BuiltInRegistries.BLOCK.getTag(requirement.tag).orElse(null)
            if (values == null || values.none()) {
                report("empty block tag ${requirement.tag.location}")
                return false
            }
        }
        return true
    }

    private fun resolvePart(part: RegistryBlockLootPart): SimulationBlockLootOutput? {
        val block = BuiltInRegistries.BLOCK.getOptional(part.block).orElse(null)
        if (block == null) {
            report("missing block ${part.block}")
            return null
        }
        var state = block.defaultBlockState()
        part.properties.forEach { (name, value) ->
            val property = block.stateDefinition.getProperty(name)
            if (property == null) {
                report("missing property '$name' on ${part.block}")
                return null
            }
            state = applyProperty(state, property, value) ?: run {
                report("invalid value '$value' for property '$name' on ${part.block}")
                return null
            }
        }
        val tool =
            part.tool
                ?.let { id -> BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR) }
                ?.takeUnless { it === Items.AIR }
                ?.let(::ItemStack)
                ?: ItemStack.EMPTY
        return SimulationBlockLootOutput(state, tool = tool, chance = part.chance, minRolls = part.minRolls, maxRolls = part.maxRolls)
    }

    private fun resolveItemOutput(part: RegistryItemOutputPart): SimulationItemOutput? {
        val item = BuiltInRegistries.ITEM.getOptional(part.item).orElse(null)
        if (item == null || item === Items.AIR) {
            report("missing item ${part.item}")
            return null
        }
        return SimulationItemOutput(ItemStack(item, part.count), part.chance, part.minRolls, part.maxRolls)
    }

    private fun report(message: String) {
        if (!loggedErrors.add(message)) return
        LazyRuntime.logger.error("Disabled {} plant simulation '{}': {}", integration, spec.id, message)
    }
}

private fun <T : Comparable<T>> applyProperty(
    state: BlockState,
    property: Property<T>,
    value: String,
): BlockState? {
    val parsed =
        if (value == MAX_INTEGER_PROPERTY && property is IntegerProperty) {
            @Suppress("UNCHECKED_CAST")
            property.possibleValues.maxOrNull() as T?
        } else {
            property.getValue(value).orElse(null)
        } ?: return null
    return state.setValue(property, parsed)
}

@LazyInternalApi
public const val MAX_INTEGER_PROPERTY: String = "@max"
