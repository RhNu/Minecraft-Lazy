package rhx.lazy.protection

internal object DamageCapPolicy {
    fun grantsInvulnerability(
        data: DamageCapData?,
        bypassesProtection: Boolean,
    ): Boolean = data?.enabled == true && data.threshold == 0 && !bypassesProtection

    fun capIncomingDamage(
        data: DamageCapData?,
        amount: Float,
        bypassesProtection: Boolean,
    ): Float {
        if (data?.enabled != true || data.threshold == 0 || bypassesProtection) {
            return amount
        }
        return minOf(amount, data.threshold.toFloat())
    }
}
