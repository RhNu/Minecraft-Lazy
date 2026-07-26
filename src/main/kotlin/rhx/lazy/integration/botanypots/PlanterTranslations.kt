package rhx.lazy.integration.botanypots

import rhx.lazy.core.datagen.LanguageContributions

internal object PlanterTranslations {
    fun register() {
        add("block.lazy.planter", "Planter", "种植机")
        add("gui.lazy.planter.pot", "Botany Pot", "盆栽")
        add("gui.lazy.planter.soil", "Soil", "土壤")
        add("gui.lazy.planter.seed", "Seed", "种子")
        add("gui.lazy.planter.growth", "Growth", "生长进度")
        add("gui.lazy.planter.network", "Dimension network output", "维度网络直送")
        add("gui.lazy.planter.downward", "Automatic downward output", "自动向下输出")
        add("gui.lazy.planter.enabled", "Enabled", "已开启")
        add("gui.lazy.planter.disabled", "Disabled", "已关闭")
        add("gui.lazy.planter.pending.title", "Waiting products", "待分发产物")
        add(
            "gui.lazy.planter.pending.paused",
            "Growth is paused until these products can be distributed.",
            "这些产物分发完毕前，种植机暂停生长。",
        )
        add("gui.lazy.planter.pending.entry", "%s × %s", "%s × %s")
        add("gui.lazy.planter.pending.more", "…and %s more product types", "……以及另外 %s 类产物")
        add(
            "message.lazy.planter.network_forwarding",
            "Dimension network output: %s",
            "维度网络直送：%s",
        )
    }

    private fun add(
        key: String,
        english: String,
        chinese: String,
    ) {
        LanguageContributions.register(key, english, chinese)
    }
}
