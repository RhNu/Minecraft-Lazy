package rhx.lazy.integration.mekanism

import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin
import mekanism.client.recipe_viewer.jei.MekanismJEI
import rhx.lazy.feature.replicator.ReplicatorGhostIngredientExtensions
import rhx.lazy.integration.annotation.LazyClientEntrypoint
import rhx.lazy.integration.api.ClientIntegration
import rhx.lazy.integration.api.IntegrationClientContext
import rhx.lazy.integration.api.IntegrationModSet

@LazyClientEntrypoint
internal object MekanismClientIntegration : ClientIntegration {
    override fun install(context: IntegrationClientContext) {
        if (IntegrationModSet.isLoaded("jei")) MekanismChemicalJeiGhostIngredient.install()
    }
}

private object MekanismChemicalJeiGhostIngredient {
    fun install() {
        ReplicatorGhostIngredientExtensions.register { element, sink ->
            LDLibJEIPlugin.ghostIngredient(
                element,
                MekanismJEI.TYPE_CHEMICAL,
                { ingredient -> !ingredient.ingredient.isEmpty },
                { stack -> ChemicalVariant.of(stack)?.let { sink.select(ChemicalResourceKind, it) } },
            )
        }
    }
}
