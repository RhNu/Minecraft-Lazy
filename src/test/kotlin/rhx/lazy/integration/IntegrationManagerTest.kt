package rhx.lazy.integration

import net.neoforged.bus.api.BusBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class IntegrationManagerTest {
    private val bus = BusBuilder.builder().build()

    @Test
    fun `loaded modules initialize once in explicit order while missing modules are skipped`() {
        val calls = mutableListOf<String>()
        val modules =
            listOf(
                recordingModule("first", calls),
                recordingModule("missing", calls),
                recordingModule("last", calls),
            )
        val manager = IntegrationManager(modules) { modId -> modId != "missing" }

        manager.initialize(bus)
        manager.initialize(bus)

        assertEquals(listOf("common:first", "common:last"), calls)
    }

    @Test
    fun `client phase is independent and also initializes once`() {
        val calls = mutableListOf<String>()
        val manager = IntegrationManager(listOf(recordingModule("loaded", calls))) { true }

        manager.initializeClient(bus, bus)
        manager.initializeClient(bus, bus)

        assertEquals(listOf("client:loaded"), calls)
    }

    @Test
    fun `runtime initialization failure names the integration and preserves the cause`() {
        val cause = IllegalArgumentException("broken API")
        val module =
            object : IntegrationModule {
                override val modId: String = "broken"

                override fun initialize(context: IntegrationContext) = throw cause
            }
        val manager = IntegrationManager(listOf(module)) { true }

        val failure =
            try {
                manager.initialize(bus)
                fail("Expected integration initialization to fail")
            } catch (exception: IllegalStateException) {
                exception
            }

        assertTrue(failure.message.orEmpty().contains("broken"))
        assertTrue(failure.cause is IllegalArgumentException)
    }

    @Test
    fun `linkage initialization failure is converted to startup failure`() {
        val module =
            object : IntegrationModule {
                override val modId: String = "unlinked"

                override fun initialize(context: IntegrationContext) = throw NoClassDefFoundError("missing.Api")
            }
        val manager = IntegrationManager(listOf(module)) { true }

        val failure =
            try {
                manager.initialize(bus)
                fail("Expected integration initialization to fail")
            } catch (exception: IllegalStateException) {
                exception
            }

        assertTrue(failure.message.orEmpty().contains("unlinked"))
        assertTrue(failure.cause is NoClassDefFoundError)
    }

    @Test
    fun `duplicate module ids are rejected`() {
        val calls = mutableListOf<String>()

        val failure =
            try {
                IntegrationManager(
                    listOf(
                        recordingModule("duplicate", calls),
                        recordingModule("duplicate", calls),
                    ),
                ) { true }
                null
            } catch (exception: IllegalArgumentException) {
                exception
            }

        assertNotNull(failure)
        assertTrue(failure.message.orEmpty().contains("unique"))
    }

    private fun recordingModule(
        modId: String,
        calls: MutableList<String>,
    ): IntegrationModule =
        object : IntegrationModule {
            override val modId: String = modId
            override val hasClientInitialization: Boolean = true

            override fun initialize(context: IntegrationContext) {
                calls += "common:$modId"
            }

            override fun initializeClient(context: ClientIntegrationContext) {
                calls += "client:$modId"
            }
        }
}
