package rhx.lazy.core.configurator

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.testing.jsonRoundTrip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModularConfiguratorDataTest {
    @Test
    fun `materials and module payloads survive codec round trip`() {
        val moduleId = ResourceLocation.fromNamespaceAndPath("lazy_test", "module")
        val payload = CompoundTag().apply { putString("value", "saved") }
        val value =
            ModularConfiguratorData.create(
                listOf(ModularConfiguratorMaterialEntry(4, ItemStack(Items.DIAMOND), 1024)),
                mapOf(moduleId to payload),
            )

        val decoded = ModularConfiguratorData.CODEC.jsonRoundTrip(value)

        assertEquals(1024, decoded.stack(4).count)
        assertEquals(Items.DIAMOND, decoded.stack(4).item)
        assertEquals("saved", decoded.modulePayload(moduleId)?.getString("value"))
    }

    @Test
    fun `normalization drops invalid and duplicate slots and clamps counts`() {
        val json =
            JsonParser.parseString(
                """
                {
                  "materials": [
                    {"slot": 2, "stack": {"id": "minecraft:diamond"}, "count": 5000},
                    {"slot": 2, "stack": {"id": "minecraft:emerald"}, "count": 10},
                    {"slot": 30, "stack": {"id": "minecraft:gold_ingot"}, "count": 10},
                    {"slot": 3, "stack": {"id": "minecraft:iron_ingot"}, "count": 0}
                  ]
                }
                """.trimIndent(),
            )

        val decoded =
            ModularConfiguratorData.CODEC
                .parse(JsonOps.INSTANCE, json)
                .result()
                .orElseThrow()

        assertEquals(1024, decoded.stack(2).count)
        assertEquals(Items.DIAMOND, decoded.stack(2).item)
        assertTrue(decoded.stack(3).isEmpty)
        assertTrue(decoded.stack(17).isEmpty)
    }

    @Test
    fun `payload access is defensive and clearing modules preserves materials`() {
        val id = ResourceLocation.fromNamespaceAndPath("lazy_test", "module")
        val original = CompoundTag().apply { putInt("answer", 42) }
        val data =
            ModularConfiguratorData
                .create(emptyList(), emptyMap())
                .withStack(0, ItemStack(Items.REDSTONE, 120))
                .withModulePayload(id, original)

        original.putInt("answer", 0)
        val read = requireNotNull(data.modulePayload(id))
        read.putInt("answer", 1)
        val cleared = data.clearModulePayloads()

        assertEquals(42, data.modulePayload(id)?.getInt("answer"))
        assertEquals(120, cleared.stack(0).count)
        assertNull(cleared.modulePayload(id))
        assertFalse(cleared.hasModulePayloads())
    }
}
