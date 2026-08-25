package rhx.lazy.core.resource

import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class ResourceAmountPersistenceTest {
    private val registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    @Test
    fun `registered resource kinds round trip without feature-specific codecs`() {
        if (ResourceKinds[TokenResourceKind.id] == null) ResourceKinds.register(TokenResourceKind)
        val original = ResourceAmount(TokenResourceKind, TokenVariant("future-resource"), 9_000_000_000L)

        val restored = requireNotNull(ResourceAmount.parse(registries, original.save(registries)))

        assertEquals(TokenResourceKind, restored.kind)
        assertEquals(9_000_000_000L, restored.amount)
        assertEquals("future-resource", (restored.variant as TokenVariant).value)
        assertNotSame(original.variant, restored.variant)
    }

    private data class TokenVariant(
        val value: String,
    ) : ResourceVariant {
        override fun copyVariant(): ResourceVariant = copy()
    }

    private object TokenResourceKind : ResourceKind<TokenVariant> {
        override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath("lazy_test", "token")
        override val displayName: Component = Component.literal("Tokens")

        override fun variantName(variant: TokenVariant): Component = Component.literal(variant.value)

        override fun matches(
            first: TokenVariant,
            second: TokenVariant,
        ): Boolean = first == second

        override fun copy(variant: TokenVariant): TokenVariant = variant.copy()

        override fun save(
            registries: HolderLookup.Provider,
            variant: TokenVariant,
        ): CompoundTag = CompoundTag().apply { putString("value", variant.value) }

        override fun parse(
            registries: HolderLookup.Provider,
            tag: CompoundTag,
        ): TokenVariant? = tag.getString("value").takeIf(String::isNotEmpty)?.let(::TokenVariant)
    }
}
