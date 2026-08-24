package rhx.lazy.integration.beyonddimensions

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import rhx.lazy.core.io.NetworkTargetRef
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BeyondDimensionsNetworkProviderTest {
    private val provider = BeyondDimensionsNetworkProvider(EmptyPort)

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

private object EmptyPort : BeyondDimensionsStoragePort {
    override fun primaryNetwork(player: ServerPlayer) = BeyondDimensionsStorageResult.NetworkNotFound

    override fun insertItems(
        networkId: BeyondDimensionsNetworkId,
        template: ItemStack,
        amount: Long,
        simulate: Boolean,
    ) = BeyondDimensionsStorageResult.Failed

    override fun insertFluid(
        networkId: BeyondDimensionsNetworkId,
        template: FluidStack,
        amount: Long,
        simulate: Boolean,
    ) = BeyondDimensionsStorageResult.Failed

    override fun insertEnergy(
        networkId: BeyondDimensionsNetworkId,
        amount: Long,
        simulate: Boolean,
    ) = BeyondDimensionsStorageResult.Failed
}
