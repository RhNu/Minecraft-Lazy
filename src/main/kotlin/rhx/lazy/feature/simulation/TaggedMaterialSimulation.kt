package rhx.lazy.feature.simulation

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import rhx.lazy.core.lazyId
import rhx.lazy.core.material.MaterialTagPreference

/**
 * One tag driven material rule: inputs carrying `<namespace>:<inputPrefix>/<material>` grow into the
 * canonical item of `<namespace>:<outputPrefix>/<material>`.
 */
internal data class TaggedMaterialRule(
    val kind: String,
    val inputPrefix: String,
    val outputPrefix: String,
    val namespace: String = "c",
) {
    init {
        require(kind.isNotBlank()) { "Tagged material rule kind must not be blank" }
        require(inputPrefix.isNotBlank() && outputPrefix.isNotBlank()) { "Tagged material rule prefixes must not be blank" }
    }

    fun material(tag: TagKey<Item>): String? {
        val id = tag.location
        if (id.namespace != namespace) return null
        val prefix = "$inputPrefix/"
        return id.path.takeIf { it.startsWith(prefix) && it.length > prefix.length }?.removePrefix(prefix)
    }

    fun outputTag(material: String): TagKey<Item> =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, "$outputPrefix/$material"))
}

internal data class TaggedMaterialMatch(
    val rule: TaggedMaterialRule,
    val material: String,
)

internal object TaggedMaterialRules {
    private val rules = linkedMapOf<String, TaggedMaterialRule>()

    init {
        register(TaggedMaterialRule("ingot", "ingots", "raw_materials"))
        register(TaggedMaterialRule("gem", "gems", "gems"))
        register(TaggedMaterialRule("dust", "dusts", "dusts"))
    }

    @Synchronized
    fun register(rule: TaggedMaterialRule) {
        require(rules.putIfAbsent(rule.kind, rule) == null) { "Duplicate tagged material rule ${rule.kind}" }
        SimulationRecipeResolver.invalidate()
    }

    fun all(): List<TaggedMaterialRule> = synchronized(this) { rules.values.toList() }

    fun matches(tags: List<TagKey<Item>>): List<TaggedMaterialMatch> =
        all()
            .flatMap { rule -> tags.mapNotNull { tag -> rule.material(tag)?.let { TaggedMaterialMatch(rule, it) } } }
            .distinct()

    fun fingerprint(): List<String> = all().map { rule -> "${rule.kind}:${rule.namespace}:${rule.inputPrefix}>${rule.outputPrefix}" }
}

internal object TaggedMaterialAdapter : AutomaticSimulationAdapter {
    val SOURCE: ResourceLocation = lazyId("material")

    override fun resolve(
        level: Level,
        stack: ItemStack,
    ): AutomaticSimulationCandidate? {
        if (!SimulationConfigs.settings.taggedMaterials.get()) return null
        val matches = TaggedMaterialRules.matches(stack.tags.toList())
        return when (matches.size) {
            0 -> duplicateSelf(stack)
            // 规则命中却查不到候选产物时（合金锭没有对应的 c:raw_materials/<材料>）回落到显式
            // opt-in，否则 duplicate_self 对这些物品静默失效。输入输出同标签的规则（gem、dust）
            // 走不到这一步：物品自己就在输出标签里，tagged 不会落空。
            1 -> tagged(matches.single()) ?: duplicateSelf(stack)
            else -> null
        }
    }

    private fun tagged(match: TaggedMaterialMatch): AutomaticSimulationCandidate? {
        val output = MaterialTagPreference.preferredItem(match.rule.outputTag(match.material)) ?: return null
        return candidate(
            automaticId(SOURCE, match.rule.kind, match.material),
            SimulationItemOutput(ItemStack(output)),
        )
    }

    private fun duplicateSelf(stack: ItemStack): AutomaticSimulationCandidate? {
        if (!stack.`is`(SimulationTags.duplicateSelfTargets)) return null
        val input = BuiltInRegistries.ITEM.getKey(stack.item)
        return candidate(
            automaticId(SOURCE, "self", input.namespace, input.path),
            SimulationItemOutput(stack.copyWithCount(1)),
        )
    }

    private fun candidate(
        id: ResourceLocation,
        output: SimulationItemOutput,
    ) = AutomaticSimulationCandidate(
        SOURCE,
        id,
        SimulationConfigs.settings.taggedMaterialDuration.get(),
        PRIORITY,
        itemOutputs = listOf(output),
    )

    private const val PRIORITY = 100
}
