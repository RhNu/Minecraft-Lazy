package rhx.lazy.integration.ae2

import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkInsertCapability
import rhx.lazy.core.io.NetworkPayload

internal interface AeStoragePayloadAdapter {
    val capability: NetworkInsertCapability

    fun convert(payload: NetworkPayload): AeStoragePayload?
}

internal data class AeStoragePayload(
    val key: AEKey,
    val amount: Long,
)

internal object AeStoragePayloadAdapters {
    private val adapters = linkedMapOf<NetworkInsertCapability, AeStoragePayloadAdapter>()

    /** Snapshot rebuilt on registration; the router reads this on every push. */
    var capabilities: Set<NetworkInsertCapability> = emptySet()
        private set

    fun register(adapter: AeStoragePayloadAdapter) {
        check(adapters.putIfAbsent(adapter.capability, adapter) == null) {
            "An AE storage adapter is already registered for ${adapter.capability.id}"
        }
        capabilities = adapters.keys.toSet()
    }

    fun convert(payload: NetworkPayload): AeStoragePayload? = adapters[payload.capability]?.convert(payload)

    fun registerAe2Adapters() {
        register(
            object : AeStoragePayloadAdapter {
                override val capability = NetworkInsertCapabilities.ITEM

                override fun convert(payload: NetworkPayload): AeStoragePayload? {
                    val items = payload as? NetworkPayload.Items ?: return null
                    val key = AEItemKey.of(items.template) ?: return null
                    return AeStoragePayload(key, items.amount)
                }
            },
        )
        register(
            object : AeStoragePayloadAdapter {
                override val capability = NetworkInsertCapabilities.FLUID

                override fun convert(payload: NetworkPayload): AeStoragePayload? {
                    val fluid = payload as? NetworkPayload.Fluid ?: return null
                    val key = AEFluidKey.of(fluid.stack) ?: return null
                    return AeStoragePayload(key, fluid.stack.amount.toLong())
                }
            },
        )
    }
}
