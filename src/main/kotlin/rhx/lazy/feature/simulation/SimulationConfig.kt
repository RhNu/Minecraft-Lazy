package rhx.lazy.feature.simulation

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.Version
import me.fzzyhmstrs.fzzy_config.annotations.WithPerms
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import rhx.lazy.core.lazyId

@Version(2)
@WithPerms(opLevel = 2)
internal class SimulationConfig : Config(lazyId("simulation")) {
    @Comment("Default simulation duration in ticks when a recipe does not specify one.")
    var defaultDuration = ValidatedInt(1200, 1..72_000)

    @Comment("Maximum virtual output rolls processed by one chamber per server tick.")
    var maxRollsPerTick = ValidatedInt(16, 1..4096)

    @Comment("Allow automatic c:ingots/material, c:gems/material, c:dusts/material, and vanilla coal simulation recipes.")
    var automaticMinerals = ValidatedBoolean(true)

    @Comment("Duration in ticks for automatically inferred mineral simulations.")
    var automaticMineralDuration = ValidatedInt(1200, 1..72_000)

    @Comment("Preferred item namespaces for automatic mineral outputs, from highest to lowest priority.")
    var automaticMineralModPriority: ValidatedList<String> =
        ValidatedList<String>(
            listOf("kubejs", "minecraft", "alltheores", "create", "mekanism", "jaopca"),
            ValidatedString(),
        )

    var t1SpeedMultiplier = ValidatedInt(1, 1..1024)
    var t1OutputMultiplier = ValidatedInt(1, 1..1024)
    var t2SpeedMultiplier = ValidatedInt(2, 1..1024)
    var t2OutputMultiplier = ValidatedInt(4, 1..1024)
    var t3SpeedMultiplier = ValidatedInt(6, 1..1024)
    var t3OutputMultiplier = ValidatedInt(12, 1..1024)
    var t4SpeedMultiplier = ValidatedInt(18, 1..1024)
    var t4OutputMultiplier = ValidatedInt(36, 1..1024)
}

internal object SimulationConfigs {
    val settings: SimulationConfig =
        ConfigApi.registerAndLoadConfig(::SimulationConfig, RegisterType.BOTH)

    fun init() = Unit
}
