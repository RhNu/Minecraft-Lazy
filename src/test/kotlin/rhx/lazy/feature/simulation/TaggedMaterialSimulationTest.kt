package rhx.lazy.feature.simulation

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import rhx.lazy.core.lazyId
import rhx.lazy.core.material.materialIdComparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaggedMaterialSimulationTest {
    @Test
    fun `built in rules map ingots to raw materials and keep gems and dusts in place`() {
        val rules = TaggedMaterialRules.all().associateBy(TaggedMaterialRule::kind)

        assertEquals(itemTag("raw_materials/iron"), rules.getValue("ingot").outputTag("iron"))
        assertEquals(itemTag("gems/amethyst"), rules.getValue("gem").outputTag("amethyst"))
        assertEquals(itemTag("dusts/glowstone"), rules.getValue("dust").outputTag("glowstone"))
    }

    @Test
    fun `rules only read their own prefix inside the common namespace`() {
        val gem = TaggedMaterialRules.all().single { it.kind == "gem" }

        assertEquals("amethyst", gem.material(itemTag("gems/amethyst")))
        assertNull(gem.material(itemTag("dusts/glowstone")))
        assertNull(gem.material(itemTag("gems/")))
        assertNull(gem.material(TagKey.create(Registries.ITEM, lazyId("gems/amethyst"))))
    }

    @Test
    fun `matching collapses duplicates and reports every matched material`() {
        assertTrue(TaggedMaterialRules.matches(listOf(itemTag("foods/bread"))).isEmpty())

        assertEquals(
            listOf(TaggedMaterialMatch(TaggedMaterialRules.all().single { it.kind == "dust" }, "glowstone")),
            TaggedMaterialRules.matches(listOf(itemTag("dusts/glowstone"), itemTag("dusts/glowstone"))),
        )

        val ambiguous = TaggedMaterialRules.matches(listOf(itemTag("ingots/iron"), itemTag("dusts/iron")))
        assertEquals(setOf("ingot" to "iron", "dust" to "iron"), ambiguous.map { it.rule.kind to it.material }.toSet())
    }

    @Test
    fun `only rules whose output differs from their input can fall back to duplicate self`() {
        val (selfOutput, derived) = TaggedMaterialRules.all().partition { it.inputPrefix == it.outputPrefix }

        // gem 与 dust 的输出标签就是输入标签，匹配到的物品必然是自己的候选，tagged 不会落空，
        // 所以 TaggedMaterialAdapter.resolve 里的 duplicate_self 回退只对下面的 derived 生效。
        assertEquals(setOf("gem", "dust"), selfOutput.map(TaggedMaterialRule::kind).toSet())
        selfOutput.forEach { rule -> assertEquals("iron", rule.material(rule.outputTag("iron"))) }

        assertEquals(setOf("ingot"), derived.map(TaggedMaterialRule::kind).toSet())
        derived.forEach { rule -> assertNull(rule.material(rule.outputTag("iron"))) }
    }

    @Test
    fun `rule kinds are unique`() {
        assertTrue(
            runCatching {
                TaggedMaterialRules.register(TaggedMaterialRule("gem", "crystals", "crystals"))
            }.exceptionOrNull() is IllegalArgumentException,
        )
    }

    @Test
    fun `tag derived candidate order uses configured namespaces then deterministic fallback`() {
        val comparator = materialIdComparator(listOf("kubejs", "minecraft", "create"))
        val ids =
            listOf(
                id("zeta", "raw_iron"),
                id("create", "raw_iron"),
                id("alpha", "raw_iron_b"),
                id("minecraft", "raw_iron"),
                id("kubejs", "raw_iron"),
                id("alpha", "raw_iron_a"),
            ).sortedWith(comparator)

        assertEquals(
            listOf(
                id("kubejs", "raw_iron"),
                id("minecraft", "raw_iron"),
                id("create", "raw_iron"),
                id("alpha", "raw_iron_a"),
                id("alpha", "raw_iron_b"),
                id("zeta", "raw_iron"),
            ),
            ids,
        )
    }

    @Test
    fun `unlisted namespaces still resolve to one deterministic winner`() {
        val comparator = materialIdComparator(listOf("kubejs"))
        val candidates = listOf(id("zeta", "raw_iron"), id("alpha", "raw_iron"), id("beta", "raw_iron"))

        assertEquals(id("alpha", "raw_iron"), candidates.minWith(comparator))
    }

    @Test
    fun `rule fingerprint changes when a rule table entry would change`() {
        assertTrue(TaggedMaterialRules.fingerprint().contains("ingot:c:ingots>raw_materials"))
        assertEquals(TaggedMaterialRules.all().size, TaggedMaterialRules.fingerprint().size)
    }

    private fun id(
        namespace: String,
        path: String,
    ) = ResourceLocation.fromNamespaceAndPath(namespace, path)

    private fun itemTag(path: String): TagKey<Item> = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path))
}
