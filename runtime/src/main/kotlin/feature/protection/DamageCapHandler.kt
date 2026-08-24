package rhx.lazy.feature.protection

import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent

internal object DamageCapHandler {
    fun onInvulnerabilityCheck(event: EntityInvulnerabilityCheckEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val data = player.getExistingDataOrNull(ProtectionRegistries.damageCap)
        val bypassesProtection = event.source.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY)
        if (DamageCapPolicy.grantsInvulnerability(data, bypassesProtection)) {
            event.isInvulnerable = true
        }
    }

    fun onIncomingDamage(event: LivingIncomingDamageEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val data = player.getExistingDataOrNull(ProtectionRegistries.damageCap)
        val bypassesProtection = event.source.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY)
        val cappedAmount = DamageCapPolicy.capIncomingDamage(data, event.amount, bypassesProtection)
        if (cappedAmount < event.amount) {
            event.amount = cappedAmount
        }
    }
}
