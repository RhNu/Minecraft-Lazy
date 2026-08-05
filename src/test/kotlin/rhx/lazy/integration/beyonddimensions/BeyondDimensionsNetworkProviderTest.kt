package rhx.lazy.integration.beyonddimensions

import net.minecraft.nbt.CompoundTag
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.testing.FakeNetworkStorage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BeyondDimensionsNetworkProviderTest {
    private val provider = BeyondDimensionsNetworkProvider(FakeNetworkStorage())

    @Test
    fun `target validation requires a typed nonnegative network id`() {
        assertFalse(provider.isTargetValid(NetworkTargetRef(provider.id, CompoundTag())))
        assertFalse(
            provider.isTargetValid(
                NetworkTargetRef(
                    provider.id,
                    CompoundTag().apply { putString("networkId", "7") },
                ),
            ),
        )
        assertFalse(
            provider.isTargetValid(
                NetworkTargetRef(
                    provider.id,
                    CompoundTag().apply { putInt("networkId", -1) },
                ),
            ),
        )
        assertTrue(
            provider.isTargetValid(
                NetworkTargetRef(
                    provider.id,
                    CompoundTag().apply { putInt("networkId", 7) },
                ),
            ),
        )
    }
}
