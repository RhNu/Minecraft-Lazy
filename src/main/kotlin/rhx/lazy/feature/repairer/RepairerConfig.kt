package rhx.lazy.feature.repairer

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.Version
import me.fzzyhmstrs.fzzy_config.annotations.WithPerms
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import rhx.lazy.core.lazyId

@Version(1)
@WithPerms(opLevel = 2)
internal class RepairerConfig : Config(lazyId("repairer")) {
    @Comment("Minimum percentage of an item's maximum durability repaired per button press.")
    var minimumRepairPercent = ValidatedInt(5, 1..100)

    @Comment("Maximum percentage of an item's maximum durability repaired per button press.")
    var maximumRepairPercent = ValidatedInt(15, 1..100)
}

internal object RepairerConfigs {
    val settings: RepairerConfig =
        ConfigApi.registerAndLoadConfig(::RepairerConfig, RegisterType.BOTH)

    fun init() = Unit
}
