package rhx.lazy.core.io

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IoPanelStateTest {
    @Test
    fun `every mode survives a round trip`() {
        IoMode.entries.forEach { mode ->
            val editor = FakeEditor(IoConfiguration(mode = mode))
            assertEquals(mode, IoPanelState.decodeMode(IoPanelState.encodeMode(editor)))
        }
    }

    @Test
    fun `every face keeps its own mode`() {
        val configuration =
            RelativeSide.entries.foldIndexed(IoConfiguration()) { index, current, side ->
                current.withSide(side, SideIoMode.entries[index % SideIoMode.entries.size])
            }
        val code = IoPanelState.encodeSides(configuration)

        RelativeSide.entries.forEach { side ->
            assertEquals(configuration.side(side), IoPanelState.decodeSide(code, side), "side $side")
        }
    }

    @Test
    fun `an unconfigured face decodes as disabled rather than as a state of its own`() {
        // The panel used to leave freshly opened faces without any state at all; the codes are
        // arranged so the default configuration is an ordinary "disabled" everywhere.
        val code = IoPanelState.encodeSides(IoConfiguration.DEFAULT)

        assertNotEquals(IoPanelState.UNSYNCED, code)
        RelativeSide.entries.forEach { side -> assertEquals(SideIoMode.NONE, IoPanelState.decodeSide(code, side)) }
    }

    @Test
    fun `no encoder can produce the unsynced seed`() {
        // A binding only fires when the synced value differs from the one it holds, so a code equal
        // to the seed would leave that group of widgets showing whatever it was built with.
        val providers = listOf(FakeProvider("first"), FakeProvider("second"))
        val editors =
            listOf(
                null,
                FakeEditor(IoConfiguration.DEFAULT),
                FakeEditor(IoConfiguration(mode = IoMode.FACE, autoEject = true)),
                FakeEditor(IoConfiguration(mode = IoMode.NETWORK, networkTarget = providers[1].target()), paused = true),
            )

        editors.forEach { editor ->
            assertNotEquals(IoPanelState.UNSYNCED, IoPanelState.encodeMode(editor))
            assertNotEquals(IoPanelState.UNSYNCED, IoPanelState.encodeSides(editor?.configuration))
            assertNotEquals(IoPanelState.UNSYNCED, IoPanelState.encodeAutoEject(editor))
            assertNotEquals(IoPanelState.UNSYNCED, IoPanelState.encodeNetwork(editor, providers))
            assertNotEquals(IoPanelState.UNSYNCED, IoPanelState.encodeCompatibility(editor, providers))
        }
    }

    @Test
    fun `auto eject round trips`() {
        assertFalse(IoPanelState.decodeAutoEject(IoPanelState.encodeAutoEject(FakeEditor(IoConfiguration.DEFAULT))))
        assertTrue(IoPanelState.decodeAutoEject(IoPanelState.encodeAutoEject(FakeEditor(IoConfiguration(autoEject = true)))))
    }

    @Test
    fun `a bound provider reports its own slot and its pause state`() {
        val providers = listOf(FakeProvider("first"), FakeProvider("second"))
        val editor = FakeEditor(IoConfiguration(mode = IoMode.NETWORK, networkTarget = providers[1].target()))

        val running = IoPanelState.encodeNetwork(editor, providers)
        assertEquals(2, IoPanelState.decodeNetworkSlot(running))
        assertFalse(IoPanelState.decodeNetworkPaused(running))

        val paused = IoPanelState.encodeNetwork(FakeEditor(editor.configuration, paused = true), providers)
        assertEquals(2, IoPanelState.decodeNetworkSlot(paused))
        assertTrue(IoPanelState.decodeNetworkPaused(paused))
    }

    @Test
    fun `an unbound machine reports no network`() {
        val providers = listOf(FakeProvider("first"))
        val code = IoPanelState.encodeNetwork(FakeEditor(IoConfiguration.DEFAULT), providers)

        assertEquals(IoPanelState.NO_NETWORK, IoPanelState.decodeNetworkSlot(code))
        assertFalse(IoPanelState.decodeNetworkPaused(code))
    }

    @Test
    fun `a target whose provider is gone stays distinct from an unbound machine`() {
        val providers = listOf(FakeProvider("first"))
        val absent = FakeProvider("removed")
        val code =
            IoPanelState.encodeNetwork(
                FakeEditor(IoConfiguration(mode = IoMode.NETWORK, networkTarget = absent.target())),
                providers,
            )
        val slot = IoPanelState.decodeNetworkSlot(code)

        assertNotEquals(IoPanelState.NO_NETWORK, slot)
        assertEquals(null, providers.getOrNull(slot - 1))
    }

    @Test
    fun `compatibility marks only the providers that accept an output the machine has`() {
        val items = FakeProvider("items", setOf(NetworkInsertCapabilities.ITEM))
        val energy = FakeProvider("energy", setOf(NetworkInsertCapabilities.ENERGY))
        val providers = listOf(items, energy)
        val editor = FakeEditor(IoConfiguration.DEFAULT, capabilities = setOf(NetworkInsertCapabilities.ENERGY))

        val mask = IoPanelState.encodeCompatibility(editor, providers)

        assertFalse(IoPanelState.decodeCompatible(mask, 0))
        assertTrue(IoPanelState.decodeCompatible(mask, 1))
    }

    private class FakeEditor(
        override val configuration: IoConfiguration,
        paused: Boolean = false,
        override val capabilities: Set<NetworkInsertCapability> = NetworkInsertCapabilities.all,
    ) : IoConfigurationEditor {
        override val networkPaused: Boolean = paused

        override fun setMode(mode: IoMode) = Unit

        override fun cycleSide(side: RelativeSide) = Unit

        override fun toggleAutoEject() = Unit

        override fun setNetworkTarget(target: NetworkTargetRef): Boolean = false

        override fun clearNetworkTarget() = Unit
    }

    private class FakeProvider(
        name: String,
        override val capabilities: Set<NetworkInsertCapability> = NetworkInsertCapabilities.all,
    ) : NetworkOutputProvider {
        override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath("lazy", "test_$name")
        override val displayName: Component = Component.literal(name)

        fun target(): NetworkTargetRef = NetworkTargetRef(id, CompoundTag())

        override fun icon(): ItemStack = ItemStack(Items.CHEST)

        override fun resolvePrimaryTarget(player: ServerPlayer): NetworkTargetResolution = NetworkTargetResolution.NotFound

        override fun isTargetValid(target: NetworkTargetRef): Boolean = target.providerId == id

        override fun insert(
            target: NetworkTargetRef,
            payload: NetworkPayload,
            simulate: Boolean,
        ): NetworkTransferResult = NetworkTransferResult.TargetMissing
    }
}
