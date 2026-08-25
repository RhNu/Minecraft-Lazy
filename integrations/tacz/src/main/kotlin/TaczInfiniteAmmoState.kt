package rhx.lazy.integration.tacz

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity

internal object TaczInfiniteAmmoState {
    @JvmStatic
    fun isEnabled(entity: LivingEntity): Boolean = entity.getExistingDataOrNull(TaczRegistries.infiniteAmmo) == true

    fun setEnabled(
        player: ServerPlayer,
        enabled: Boolean,
    ) {
        if (enabled) {
            player.setData(TaczRegistries.infiniteAmmo, true)
        } else {
            player.removeData(TaczRegistries.infiniteAmmo)
        }
    }
}
