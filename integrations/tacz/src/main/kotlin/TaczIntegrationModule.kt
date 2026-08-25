package rhx.lazy.integration.tacz

import rhx.lazy.core.command.LazyCommands
import rhx.lazy.integration.annotation.LazyCommonEntrypoint
import rhx.lazy.integration.api.CommonIntegration
import rhx.lazy.integration.api.IntegrationCommonContext

@LazyCommonEntrypoint
internal object TaczIntegrationModule : CommonIntegration {
    override fun install(context: IntegrationCommonContext) {
        TaczRegistries.register(context.modBus)
        LazyCommands.contribute("tacz", TaczCommand)
    }
}
