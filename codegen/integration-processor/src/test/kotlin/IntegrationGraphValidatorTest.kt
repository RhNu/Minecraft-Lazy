package rhx.lazy.integration.processor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntegrationGraphValidatorTest {
    @Test
    fun `orders dependencies before consumers`() {
        assertEquals(
            listOf("ae2", "appflux"),
            IntegrationGraphValidator.topologicalOrder(
                listOf(
                    IntegrationGraphNode("appflux", setOf("ae2")),
                    IntegrationGraphNode("ae2", emptySet()),
                ),
            ),
        )
    }

    @Test
    fun `rejects unknown dependencies`() {
        assertFailsWith<IllegalArgumentException> {
            IntegrationGraphValidator.topologicalOrder(listOf(IntegrationGraphNode("appflux", setOf("ae2"))))
        }
    }

    @Test
    fun `rejects cycles`() {
        assertFailsWith<IllegalArgumentException> {
            IntegrationGraphValidator.topologicalOrder(
                listOf(
                    IntegrationGraphNode("first", setOf("second")),
                    IntegrationGraphNode("second", setOf("first")),
                ),
            )
        }
    }

    @Test
    fun `rejects duplicate ids`() {
        assertFailsWith<IllegalArgumentException> {
            IntegrationGraphValidator.topologicalOrder(
                listOf(
                    IntegrationGraphNode("ae2", emptySet()),
                    IntegrationGraphNode("ae2", emptySet()),
                ),
            )
        }
    }

    @Test
    fun `uses stable id ordering for independent integrations`() {
        assertEquals(
            listOf("ae2", "curios", "jade"),
            IntegrationGraphValidator.topologicalOrder(
                listOf(
                    IntegrationGraphNode("jade", emptySet()),
                    IntegrationGraphNode("ae2", emptySet()),
                    IntegrationGraphNode("curios", emptySet()),
                ),
            ),
        )
    }
}
