package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.resource.EnergyVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKinds
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.feature.energy.ENERGY_TRANSFER_LIMIT
import rhx.lazy.feature.energy.EnergyRegistries
import rhx.lazy.feature.energy.EnergySourceBlockEntity
import rhx.lazy.feature.simulation.SimulationChamberBlockEntity
import rhx.lazy.feature.simulation.SimulationRegistries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IoControllerTest {
    @Test
    fun `network target keeps provider and opaque data`() {
        val provider = RecordingProvider("opaque")
        NetworkOutputProviders.register(provider)
        val source = newSource()

        assertTrue(source.ioController.setNetworkTarget(provider.target("value")))
        source.onServerTick()

        assertEquals(IoMode.NETWORK, source.ioController.mode)
        assertEquals(
            "value",
            source.ioController.target
                ?.data
                ?.getString("opaque"),
        )
        assertEquals(ENERGY_TRANSFER_LIMIT.toLong(), provider.energyAmount)
    }

    @Test
    fun `temporary failure keeps mode and retries later`() {
        val provider = RecordingProvider("temporary")
        NetworkOutputProviders.register(provider)
        val source = newSource()
        assertTrue(source.ioController.setNetworkTarget(provider.target("retry")))
        provider.result = TransferResult.TemporarilyUnavailable

        source.onServerTick()
        assertEquals(IoMode.NETWORK, source.ioController.mode)
        assertFalse(source.ioController.networkPaused)
        val attempts = provider.attempts
        repeat(19) { source.onServerTick() }
        assertEquals(attempts, provider.attempts)
        provider.result = TransferResult.Accepted(0)
        source.onServerTick()
        assertTrue(provider.attempts > attempts)
    }

    @Test
    fun `missing target falls back to passive`() {
        val provider = RecordingProvider("missing")
        NetworkOutputProviders.register(provider)
        val source = newSource()
        assertTrue(source.ioController.setNetworkTarget(provider.target("missing")))
        provider.result = TransferResult.TargetMissing

        source.onServerTick()

        assertEquals(IoMode.PASSIVE, source.ioController.mode)
    }

    @Test
    fun `unknown outcome pauses until target is selected again`() {
        val provider = RecordingProvider("unknown")
        NetworkOutputProviders.register(provider)
        val source = newSource()
        val target = provider.target("unknown")
        assertTrue(source.ioController.setNetworkTarget(target))
        provider.result = TransferResult.OutcomeUnknown

        source.onServerTick()
        assertTrue(source.ioController.networkPaused)
        val attempts = provider.attempts
        provider.result = TransferResult.Accepted(0)
        source.onServerTick()
        assertEquals(attempts, provider.attempts)

        assertTrue(source.ioController.setNetworkTarget(target))
        source.onServerTick()
        assertFalse(source.ioController.networkPaused)
    }

    @Test
    fun `disconnecting clears the target and returns to passive`() {
        val provider = RecordingProvider("disconnect")
        NetworkOutputProviders.register(provider)
        val source = newSource()
        assertTrue(source.ioController.setNetworkTarget(provider.target("bound")))

        source.ioController.clearNetworkTarget()

        assertEquals(IoMode.PASSIVE, source.ioController.mode)
        assertNull(source.ioController.target)
    }

    @Test
    fun `output only machines skip the input face states`() {
        val source = newSource()
        source.ioController.setMode(IoMode.FACE)

        source.ioController.cycleSide(RelativeSide.TOP)
        assertEquals(SideIoMode.OUTPUT, source.ioController.configuration.side(RelativeSide.TOP))

        source.ioController.cycleSide(RelativeSide.TOP)
        assertEquals(SideIoMode.NONE, source.ioController.configuration.side(RelativeSide.TOP))
    }

    @Test
    fun `simulation chamber faces only cycle output states`() {
        val chamber = SimulationChamberBlockEntity(BlockPos.ZERO, SimulationRegistries.block.get().defaultBlockState())
        chamber.ioController.setMode(IoMode.FACE)

        chamber.ioController.cycleSide(RelativeSide.TOP)
        assertEquals(SideIoMode.OUTPUT, chamber.ioController.configuration.side(RelativeSide.TOP))

        chamber.ioController.cycleSide(RelativeSide.TOP)
        assertEquals(SideIoMode.NONE, chamber.ioController.configuration.side(RelativeSide.TOP))
    }

    @Test
    fun `temporarily missing provider preserves a saved network target`() {
        val source = newSource()
        val target =
            NetworkTargetRef(
                ResourceLocation.fromNamespaceAndPath("lazy", "temporarily_missing"),
                CompoundTag().apply { putString("opaque", "preserved") },
            )

        assertTrue(source.ioController.applyConfiguration(IoConfiguration(mode = IoMode.NETWORK, networkTarget = target)))

        assertEquals(IoMode.NETWORK, source.ioController.mode)
        assertEquals(target, source.ioController.target)
    }

    private fun newSource() =
        EnergySourceBlockEntity(
            BlockPos.ZERO,
            EnergyRegistries.sourceBlock.get().defaultBlockState(),
        )

    private class RecordingProvider(
        suffix: String,
    ) : NetworkOutputProvider {
        override val id = ResourceLocation.fromNamespaceAndPath("lazy", "test_io_$suffix")
        override val displayName: Component = Component.literal("Test $suffix")
        override val capabilities = setOf(ResourceKinds.ENERGY)
        var result: TransferResult? = null
        var attempts = 0
        var energyAmount = 0L

        override fun icon() = ItemStack(Items.CHEST)

        override fun resolvePrimaryTarget(player: ServerPlayer) = NetworkTargetResolution.Unavailable

        override fun isTargetValid(target: NetworkTargetRef) = target.providerId == id && target.data.getString("opaque").isNotBlank()

        override fun offer(
            target: NetworkTargetRef,
            amount: ResourceAmount<out ResourceVariant>,
            simulate: Boolean,
        ): TransferResult {
            attempts++
            val response = result ?: TransferResult.Accepted(amount.amount)
            if (amount.variant == EnergyVariant && response is TransferResult.Accepted && !simulate) {
                energyAmount += response.accepted.coerceIn(0L, amount.amount)
            }
            return response
        }

        fun target(value: String) = NetworkTargetRef(id, CompoundTag().apply { putString("opaque", value) })
    }
}
