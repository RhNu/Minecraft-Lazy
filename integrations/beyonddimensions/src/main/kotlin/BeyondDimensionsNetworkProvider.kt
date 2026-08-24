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
import rhx.lazy.core.io.NetworkOutputProvider
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTargetResolution
import rhx.lazy.core.io.ResourceKinds
import rhx.lazy.core.io.TransferResult
import rhx.lazy.core.resource.EnergyVariant
import rhx.lazy.core.resource.FluidVariant
import rhx.lazy.core.resource.ItemVariant
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceVariant

internal class BeyondDimensionsNetworkProvider(
    private val storage: BeyondDimensionsStoragePort,
) : NetworkOutputProvider {
    override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, "beyonddimensions")
    override val displayName: Component = Component.translatable("gui.lazy.io.provider.beyonddimensions")
    override val capabilities = ResourceKinds.all

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

    override fun offer(
        target: NetworkTargetRef,
        amount: ResourceAmount<out ResourceVariant>,
        simulate: Boolean,
    ): TransferResult {
        val networkId = networkId(target) ?: return TransferResult.InvalidTarget
        return when (val variant = amount.variant) {
            is ItemVariant ->
                when (
                    val result =
                        storage.insertItems(
                            networkId,
                            variant.template,
                            amount.amount,
                            simulate,
                        )
                ) {
                    is BeyondDimensionsStorageResult.Success ->
                        TransferResult.Accepted(amount.amount - result.value.coerceIn(0L, amount.amount))
                    BeyondDimensionsStorageResult.NetworkNotFound -> TransferResult.TargetMissing
                    BeyondDimensionsStorageResult.OutcomeUnknown -> TransferResult.OutcomeUnknown
                    else -> TransferResult.TemporarilyUnavailable
                }

            is FluidVariant ->
                when (val result = storage.insertFluid(networkId, variant.template, amount.amount, simulate)) {
                    is BeyondDimensionsStorageResult.Success ->
                        TransferResult.Accepted(amount.amount - result.value.coerceIn(0L, amount.amount))
                    BeyondDimensionsStorageResult.NetworkNotFound -> TransferResult.TargetMissing
                    BeyondDimensionsStorageResult.OutcomeUnknown -> TransferResult.OutcomeUnknown
                    else -> TransferResult.TemporarilyUnavailable
                }

            EnergyVariant ->
                when (val result = storage.insertEnergy(networkId, amount.amount, simulate)) {
                    is BeyondDimensionsStorageResult.Success ->
                        TransferResult.Accepted(amount.amount - result.value.coerceIn(0L, amount.amount))
                    BeyondDimensionsStorageResult.NetworkNotFound -> TransferResult.TargetMissing
                    BeyondDimensionsStorageResult.OutcomeUnknown -> TransferResult.OutcomeUnknown
                    else -> TransferResult.TemporarilyUnavailable
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
