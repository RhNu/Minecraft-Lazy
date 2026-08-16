package rhx.lazy.integration.mysticalagriculture

import rhx.lazy.core.datagen.LanguageContributions

internal object EssenceConverterTranslations {
    fun register() {
        add("block.lazy.essence_converter", "Essence Converter", "精华转换器")
        add("gui.lazy.essence_converter.target.unset", "Not selected", "未选择")
        add("gui.lazy.essence_converter.requires_agradditions", "Requires Mystical Agradditions", "需要 Mystical Agradditions")
        add("gui.lazy.essence_converter.select_target", "Select a target essence", "请选择目标精华")
        add("gui.lazy.essence_converter.unavailable", "Essence Converter is no longer available", "精华转换器已不可用")
        add("gui.lazy.essence_converter.amount.tooltip", "Essence amount: %s / %s", "精华量：%s / %s")
        add("gui.lazy.essence_converter.remainder.tooltip", "Remainder: %s / %s essence", "余数：%s / %s 精华量")
        add("gui.lazy.essence_converter.input.tooltip", "Insert essence", "放入精华")
        add("gui.lazy.essence_converter.clear", "Clear all stored essence", "清空全部精华")
        add("gui.lazy.essence_converter.clear.confirm", "Destroy all stored essence?", "销毁全部已存精华？")
        add("gui.lazy.essence_converter.clear.accept", "Clear", "清空")
        add("gui.lazy.essence_converter.clear.cancel", "Cancel", "取消")
        add("message.lazy.essence_converter.target_locked", "Empty the converter before changing its target", "请先清空转换器再更改目标")
        add("tooltip.lazy.essence_converter.contents", "%s: %s, remainder %s essence", "%s：%s，余数 %s 精华量")
        add("config.jade.plugin_lazy.essence_converter", "Essence Converter status", "精华转换器状态")
        add("jade.lazy.essence_converter.target", "Target: %s", "目标：%s")
        add("jade.lazy.essence_converter.contents", "Essence: %s • remainder: %s", "精华：%s • 余量：%s")
        add("lazy.essence_converter", "Essence Converter", "精华转换器")
        add("lazy.essence_converter.desc", "Server-authoritative Essence Converter settings", "由服务端控制的精华转换器设置")
        add("lazy.essence_converter.maxStoredEssence", "Maximum stored essence", "最大精华缓存")
        add(
            "lazy.essence_converter.maxStoredEssence.desc",
            "Maximum complete target essences stored by one converter.",
            "每台转换器可存储的完整目标精华上限。",
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
