package rhx.lazy.core.io

/**
 * Wire format for the IO panel's server-to-client state.
 *
 * A binding only notifies its listener when the synced value differs from the one it already holds,
 * so every code here is non-negative and the panel seeds each binding with [UNSYNCED]: the first
 * sync then always differs, and the panel can never be left showing the appearance it was built
 * with. Encoder and decoder live side by side because a disagreement between them stays invisible
 * until it reaches a player's screen.
 *
 * Each code covers one whole group of widgets, so a group is always painted from a single instant
 * of machine state rather than from a mix of updates that arrived separately.
 */
internal object IoPanelState {
    /** Seed for a binding that has not received its first sync; no encoder returns it. */
    const val UNSYNCED = -1

    /** Network slot for a machine that is not bound to any network. */
    const val NO_NETWORK = 0

    fun encodeMode(editor: IoConfigurationEditor?): Int = (editor?.configuration?.mode ?: IoConfiguration.DEFAULT.mode).ordinal

    fun decodeMode(code: Int): IoMode = IoMode.entries.getOrElse(code) { IoConfiguration.DEFAULT.mode }

    /** Two bits per side, in [RelativeSide] order. */
    fun encodeSides(configuration: IoConfiguration?): Int {
        val sides = configuration ?: return 0
        return RelativeSide.entries.fold(0) { code, side -> code or (sides.side(side).ordinal shl (side.ordinal * SIDE_BITS)) }
    }

    fun decodeSide(
        code: Int,
        side: RelativeSide,
    ): SideIoMode = SideIoMode.entries.getOrElse((code shr (side.ordinal * SIDE_BITS)) and SIDE_MASK) { SideIoMode.NONE }

    fun encodeAutoEject(editor: IoConfigurationEditor?): Int = if (editor?.configuration?.autoEject == true) 1 else 0

    fun decodeAutoEject(code: Int): Boolean = code != 0

    /**
     * [NO_NETWORK] when nothing is bound, otherwise the bound provider's position in [providers]
     * plus one, or one past the list when that provider is no longer registered. The pause flag
     * travels along so the status line and the two actions always describe the same instant.
     */
    fun encodeNetwork(
        editor: IoConfigurationEditor?,
        providers: List<NetworkOutputProvider>,
    ): Int {
        if (editor == null) return NO_NETWORK
        val target = editor.configuration.networkTarget
        val slot =
            if (target == null) {
                NO_NETWORK
            } else {
                providers.indexOfFirst { it.id == target.providerId }.let { index -> if (index < 0) providers.size + 1 else index + 1 }
            }
        return slot or if (editor.networkPaused) NETWORK_PAUSED_FLAG else 0
    }

    fun decodeNetworkSlot(code: Int): Int = code and NETWORK_SLOT_MASK

    fun decodeNetworkPaused(code: Int): Boolean = code and NETWORK_PAUSED_FLAG != 0

    /** One bit per provider, set when the machine has an output that provider can accept. */
    fun encodeCompatibility(
        editor: IoConfigurationEditor?,
        providers: List<NetworkOutputProvider>,
    ): Int {
        val capabilities = editor?.capabilities ?: return 0
        return providers.foldIndexed(0) { index, mask, provider ->
            if (capabilities.any { it in provider.capabilities }) mask or (1 shl index) else mask
        }
    }

    fun decodeCompatible(
        mask: Int,
        index: Int,
    ): Boolean = mask and (1 shl index) != 0

    private const val SIDE_BITS = 2
    private const val SIDE_MASK = 0b11
    private const val NETWORK_SLOT_MASK = 0xFF
    private const val NETWORK_PAUSED_FLAG = 1 shl 8
}
