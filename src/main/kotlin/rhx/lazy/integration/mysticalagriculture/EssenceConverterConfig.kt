package rhx.lazy.integration.mysticalagriculture

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.Version
import me.fzzyhmstrs.fzzy_config.annotations.WithPerms
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedLong
import rhx.lazy.core.lazyId

@Version(1)
@WithPerms(opLevel = 2)
internal class EssenceConverterConfig : Config(lazyId("essence_converter")) {
    @Comment("Maximum complete target essences stored by one Essence Converter.")
    var maxStoredEssence = ValidatedLong(DEFAULT_CAPACITY, 1L..Long.MAX_VALUE)

    companion object {
        const val DEFAULT_CAPACITY = 1_000_000_000_000L
    }
}

internal object EssenceConverterConfigs {
    val settings: EssenceConverterConfig =
        ConfigApi.registerAndLoadConfig(::EssenceConverterConfig, RegisterType.BOTH)

    fun init() = Unit
}
