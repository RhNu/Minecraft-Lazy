package rhx.lazy.integration.tacz

import net.neoforged.neoforge.data.event.GatherDataEvent
import rhx.lazy.core.datagen.LanguageContributions
import rhx.lazy.integration.annotation.LazyDataGenContribution

@LazyDataGenContribution(integrationId = "tacz")
internal object TaczTranslations {
    fun gatherData(event: GatherDataEvent) {
        LanguageContributions.register(
            "message.lazy.tacz.infammo.disabled",
            "Disabled",
            "已关闭",
        )
        LanguageContributions.register(
            "message.lazy.tacz.infammo.enabled",
            "Enabled",
            "已开启",
        )
        LanguageContributions.register(
            "message.lazy.tacz.infammo.off",
            "TACZ infinite ammo disabled",
            "TACZ 无限弹药已关闭",
        )
        LanguageContributions.register(
            "message.lazy.tacz.infammo.on",
            "TACZ infinite ammo enabled",
            "TACZ 无限弹药已开启",
        )
        LanguageContributions.register(
            "message.lazy.tacz.infammo.player_only",
            "This command can only be used by a player",
            "该命令只能由玩家执行",
        )
        LanguageContributions.register(
            "message.lazy.tacz.infammo.reset",
            "TACZ infinite ammo settings reset",
            "TACZ 无限弹药设置已重置",
        )
        LanguageContributions.register(
            "message.lazy.tacz.infammo.status",
            "TACZ infinite ammo: %s",
            "TACZ 无限弹药：%s",
        )
    }
}
