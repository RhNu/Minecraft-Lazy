package rhx.lazy.core.io

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.feature.energy.ENERGY_TRANSFER_LIMIT
import rhx.lazy.feature.energy.EnergyRegistries
import rhx.lazy.feature.energy.EnergySourceBlockEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        provider.result = NetworkTransferResult.TemporarilyUnavailable

        source.onServerTick()
        assertEquals(IoMode.NETWORK, source.ioController.mode)
        assertFalse(source.ioController.networkPaused)
        val attempts = provider.attempts
        repeat(19) { source.onServerTick() }
        assertEquals(attempts, provider.attempts)
        provider.result = NetworkTransferResult.Success(0)
        source.onServerTick()
        assertTrue(provider.attempts > attempts)
    }

    @Test
    fun `missing target falls back to passive`() {
        val provider = RecordingProvider("missing")
        NetworkOutputProviders.register(provider)
        val source = newSource()
        assertTrue(source.ioController.setNetworkTarget(provider.target("missing")))
        provider.result = NetworkTransferResult.TargetMissing

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
        provider.result = NetworkTransferResult.OutcomeUnknown

        source.onServerTick()
        assertTrue(source.ioController.networkPaused)
        val attempts = provider.attempts
        provider.result = NetworkTransferResult.Success(0)
        source.onServerTick()
        assertEquals(attempts, provider.attempts)

        assertTrue(source.ioController.setNetworkTarget(target))
        source.onServerTick()
        assertFalse(source.ioController.networkPaused)
    }

    @Test
    fun `passive mode still runs adapter maintenance`() {
        val entity = PassiveMaintenanceEntity()

        entity.ioController.tick()

        assertEquals(1, entity.maintenanceTicks)
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
        override val capabilities = setOf(NetworkInsertCapabilities.ENERGY)
        var result: NetworkTransferResult = NetworkTransferResult.Success(0)
        var attempts = 0
        var energyAmount = 0L

        override fun icon() = ItemStack(Items.CHEST)

        override fun resolvePrimaryTarget(player: ServerPlayer) = NetworkTargetResolution.Unavailable

        override fun isTargetValid(target: NetworkTargetRef) = target.providerId == id && target.data.getString("opaque").isNotBlank()

        override fun insert(
            target: NetworkTargetRef,
            payload: NetworkPayload,
            simulate: Boolean,
        ): NetworkTransferResult {
            attempts++
            if (payload is NetworkPayload.Energy && !simulate) energyAmount += payload.amount
            return result
        }

        fun target(value: String) = NetworkTargetRef(id, CompoundTag().apply { putString("opaque", value) })
    }

    private class PassiveMaintenanceEntity :
        IoManagedBlockEntity(
            EnergyRegistries.sourceBlockEntity.get(),
            BlockPos.ZERO,
            EnergyRegistries.sourceBlock.get().defaultBlockState(),
        ) {
        var maintenanceTicks = 0

        init {
            installIoAdapter(
                object : IoAdapter {
                    override val capabilities: Set<NetworkInsertCapability> = emptySet()
                    override val ticksWhenPassive = true

                    override fun push(configuration: IoConfiguration): IoPushResult {
                        maintenanceTicks++
                        return IoPushResult.Success
                    }
                },
            )
        }
    }
}
