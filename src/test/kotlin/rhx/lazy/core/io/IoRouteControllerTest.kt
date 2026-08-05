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

class IoRouteControllerTest {
    @Test
    fun `network target keeps provider and opaque data`() {
        val first = RecordingProvider("first")
        val second = RecordingProvider("second")
        NetworkOutputProviders.register(first)
        NetworkOutputProviders.register(second)
        val source = newSource()
        val target = first.target("opaque-value")

        assertTrue(source.ioController.setNetworkTarget(target))
        source.onServerTick()

        assertEquals(IoRoute.NETWORK, source.ioController.route)
        assertEquals(first.id, source.ioController.target?.providerId)
        assertEquals(
            "opaque-value",
            source.ioController.target
                ?.data
                ?.getString("opaque"),
        )
        assertEquals(2, NetworkOutputProviders.all().count { it.id == first.id || it.id == second.id })
        assertEquals(ENERGY_TRANSFER_LIMIT.toLong(), first.energyAmount)
        assertEquals(0L, second.energyAmount)
    }

    @Test
    fun `temporary failure keeps route and retries later`() {
        val provider = RecordingProvider("temporary")
        NetworkOutputProviders.register(provider)
        val source = newSource()
        assertTrue(source.ioController.setNetworkTarget(provider.target("retry")))
        provider.result = NetworkTransferResult.TemporarilyUnavailable

        source.onServerTick()

        assertEquals(IoRoute.NETWORK, source.ioController.route)
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

        assertEquals(IoRoute.PASSIVE, source.ioController.route)
        assertFalse(source.ioController.networkPaused)
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

        assertEquals(IoRoute.NETWORK, source.ioController.route)
        assertTrue(source.ioController.networkPaused)
        val attempts = provider.attempts
        provider.result = NetworkTransferResult.Success(0)
        source.onServerTick()
        assertEquals(attempts, provider.attempts)

        assertTrue(source.ioController.setNetworkTarget(target))
        source.onServerTick()
        assertFalse(source.ioController.networkPaused)
        assertTrue(provider.attempts > attempts)
    }

    @Test
    fun `passive route still runs adapter maintenance`() {
        val entity = PassiveMaintenanceEntity()

        entity.ioController.tick()

        assertEquals(1, entity.maintenanceTicks)
    }

    @Test
    fun `exposed target data is a defensive copy`() {
        val provider = RecordingProvider("defensive_copy")
        NetworkOutputProviders.register(provider)
        val source = newSource()
        assertTrue(source.ioController.setNetworkTarget(provider.target("original")))

        source.ioController.target
            ?.data
            ?.putString("opaque", "mutated")

        assertEquals(
            "original",
            source.ioController.target
                ?.data
                ?.getString("opaque"),
        )
    }

    private fun newSource(): EnergySourceBlockEntity =
        EnergySourceBlockEntity(
            BlockPos.ZERO,
            EnergyRegistries.sourceBlock.get().defaultBlockState(),
        )

    private class RecordingProvider(
        suffix: String,
    ) : NetworkOutputProvider {
        override val id: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath("lazy", "test_io_$suffix")
        override val displayName: Component = Component.literal("Test $suffix")
        override val supportedResourceKinds: Set<IoResourceKind> = setOf(IoResourceKind.ENERGY)

        override fun icon(): ItemStack = ItemStack(Items.CHEST)

        var result: NetworkTransferResult = NetworkTransferResult.Success(0)
        var attempts = 0
        var energyAmount = 0L

        override fun resolvePrimaryTarget(player: ServerPlayer): NetworkTargetResolution = NetworkTargetResolution.Unavailable

        override fun isTargetValid(target: NetworkTargetRef): Boolean =
            target.providerId == id && target.data.getString("opaque").isNotBlank()

        override fun insert(
            target: NetworkTargetRef,
            payload: NetworkPayload,
            simulate: Boolean,
        ): NetworkTransferResult {
            attempts++
            if (payload is NetworkPayload.Energy && !simulate) energyAmount += payload.amount
            return result
        }

        fun target(value: String): NetworkTargetRef =
            NetworkTargetRef(
                id,
                CompoundTag().apply { putString("opaque", value) },
            )
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
                object : IoRouteAdapter {
                    override val supportedRoutes: Set<IoRoute> = setOf(IoRoute.PASSIVE)
                    override val resourceKinds: Set<IoResourceKind> = emptySet()
                    override val ticksWhenPassive: Boolean = true

                    override fun push(
                        route: IoRoute,
                        target: NetworkTargetRef?,
                    ): IoPushResult {
                        maintenanceTicks++
                        return IoPushResult.Success
                    }
                },
            )
        }
    }
}
