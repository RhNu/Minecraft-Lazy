package rhx.lazy.core.material

import net.minecraft.resources.ResourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MaterialFormTest {
    @Test
    fun `form extracts material from its literal tag prefix`() {
        val storage = MaterialForm("c:storage_blocks/", 1296)
        val rawStorage = MaterialForm("c:storage_blocks/raw_", 1296)

        assertEquals("raw_iron", storage.materialOf(ResourceLocation.parse("c:storage_blocks/raw_iron")))
        assertEquals("iron", rawStorage.materialOf(ResourceLocation.parse("c:storage_blocks/raw_iron")))
        assertNull(rawStorage.materialOf(ResourceLocation.parse("c:storage_blocks/iron")))
    }

    @Test
    fun `form builds the corresponding item tag`() {
        val plate = MaterialForm("c:plates/", 144)

        assertEquals("c:plates/iron", plate.tagFor("iron")?.location.toString())
        assertNull(plate.tagFor("invalid material"))
    }
}
