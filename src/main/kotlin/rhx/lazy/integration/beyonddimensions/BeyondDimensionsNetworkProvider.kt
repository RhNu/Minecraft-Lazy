package rhx.lazy.integration.beyonddimensions

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.MOD_ID
import rhx.lazy.core.io.NetworkInsertCapabilities
import rhx.lazy.core.io.NetworkOutputProvider
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTargetResolution
import rhx.lazy.core.io.NetworkTransferResult

internal class BeyondDimensionsNetworkProvider(
    private val storage: BeyondDimensionsStoragePort,
) : NetworkOutputProvider {
    override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, "beyonddimensions")
    override val displayName: Component = Component.translatable("gui.lazy.io.provider.beyonddimensions")
    override val capabilities = NetworkInsertCapabilities.all

    override fun icon(): ItemStack =
        BuiltInRegistries.ITEM
            .getOptional(NETWORK_GENERATOR_ID)
            .map(::ItemStack)
            .orElseGet { ItemStack(Items.CHEST) }

    override fun resolvePrimaryTarget(player: ServerPlayer): NetworkTargetResolution =
        when (val result = storage.primaryNetwork(player)) {
            is BeyondDimensionsStorageResult.Success ->
                NetworkTargetResolution.Success(
                    NetworkTargetRef(
                        id,
                        CompoundTag().apply { putInt(NETWORK_ID_TAG, result.value.value) },
                    ),
                )

            BeyondDimensionsStorageResult.NetworkNotFound -> NetworkTargetResolution.NotFound
            else -> NetworkTargetResolution.Failed
        }

    override fun isTargetValid(target: NetworkTargetRef): Boolean = networkId(target) != null

    override fun insert(
        target: NetworkTargetRef,
        payload: NetworkPayload,
        simulate: Boolean,
    ): NetworkTransferResult {
        val networkId = networkId(target) ?: return NetworkTransferResult.InvalidTarget
        return when (payload) {
            is NetworkPayload.Items ->
                when (
                    val result =
                        storage.insertItems(
                            networkId,
                            payload.template,
                            payload.amount,
                            simulate,
                        )
                ) {
                    is BeyondDimensionsStorageResult.Success ->
                        NetworkTransferResult.Success(result.value.coerceIn(0L, payload.amount))
                    BeyondDimensionsStorageResult.NetworkNotFound -> NetworkTransferResult.TargetMissing
                    BeyondDimensionsStorageResult.OutcomeUnknown -> NetworkTransferResult.OutcomeUnknown
                    else -> NetworkTransferResult.TemporarilyUnavailable
                }

            is NetworkPayload.Fluid ->
                when (val result = storage.insertFluid(networkId, payload.stack, simulate)) {
                    is BeyondDimensionsStorageResult.Success ->
                        NetworkTransferResult.Success(result.value.coerceIn(0L, payload.stack.amount.toLong()))
                    BeyondDimensionsStorageResult.NetworkNotFound -> NetworkTransferResult.TargetMissing
                    BeyondDimensionsStorageResult.OutcomeUnknown -> NetworkTransferResult.OutcomeUnknown
                    else -> NetworkTransferResult.TemporarilyUnavailable
                }

            is NetworkPayload.Energy ->
                when (val result = storage.insertEnergy(networkId, payload.amount, simulate)) {
                    is BeyondDimensionsStorageResult.Success ->
                        NetworkTransferResult.Success(result.value.coerceIn(0L, payload.amount))
                    BeyondDimensionsStorageResult.NetworkNotFound -> NetworkTransferResult.TargetMissing
                    BeyondDimensionsStorageResult.OutcomeUnknown -> NetworkTransferResult.OutcomeUnknown
                    else -> NetworkTransferResult.TemporarilyUnavailable
                }
        }
    }

    private fun networkId(target: NetworkTargetRef): BeyondDimensionsNetworkId? {
        if (target.providerId != id || !target.data.contains(NETWORK_ID_TAG, Tag.TAG_INT.toInt())) return null
        val value = target.data.getInt(NETWORK_ID_TAG)
        return value.takeIf { it >= 0 }?.let(::BeyondDimensionsNetworkId)
    }

    private companion object {
        const val NETWORK_ID_TAG = "networkId"
        val NETWORK_GENERATOR_ID: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath("beyonddimensions", "net_creater")
    }
}
