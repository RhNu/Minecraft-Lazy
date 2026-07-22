package rhx.lazy.config

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.WithPerms
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import rhx.lazy.util.lazyId

@WithPerms(opLevel = 2)
internal class TeleporterConfig : Config(lazyId("teleporter")) {
    @Comment("Ticks the teleporter must be charged before it activates.")
    var chargeTicks = ValidatedInt(20, 1..72_000)

    @Comment("Cooldown in seconds after a successful teleport.")
    var cooldownSeconds = ValidatedInt(5, 0..3_600)

    @Comment("Horizontal radius searched for a safe destination.")
    var safeSearchRadius = ValidatedInt(8, 0..16)

    @Comment("Allow the teleporter to add a small platform in the void dimension.")
    var createVoidSafetyPlatform = ValidatedBoolean(true)
}

internal object ModConfig {
    val teleporter: TeleporterConfig =
        ConfigApi.registerAndLoadConfig(::TeleporterConfig, RegisterType.BOTH)

    fun init() = Unit
}
