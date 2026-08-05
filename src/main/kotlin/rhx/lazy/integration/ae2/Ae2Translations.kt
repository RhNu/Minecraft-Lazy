package rhx.lazy.integration.ae2

import rhx.lazy.core.datagen.LanguageContributions

internal object Ae2Translations {
    fun register() {
        add("item.lazy.me_output_link_card", "ME Output Link Card", "ME 输出链接卡")
        add("gui.lazy.io.provider.ae2", "AE2 ME Network", "AE2 ME 网络")
        add("tooltip.lazy.me_output_link_card.unlinked", "Unlinked — link in a Wireless Access Point", "未链接——请在无线接入点中链接")
        add("tooltip.lazy.me_output_link_card.linked", "Linked: %s", "已链接：%s")
        add("tooltip.lazy.me_output_link_card.position", "Position: %s, %s, %s", "坐标：%s, %s, %s")
        add("message.lazy.me_output_link_card.success", "ME output target set to %s, %s, %s", "ME 输出目标已设为 %s, %s, %s")
        add("message.lazy.me_output_link_card.unlinked", "This ME Output Link Card is not linked", "这张 ME 输出链接卡尚未链接")
        add(
            "message.lazy.me_output_link_card.incompatible",
            "This machine cannot output a compatible resource to that ME network",
            "该机器无法向此 ME 网络输出兼容资源",
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
