package rhx.lazy.integration.ae2

import appeng.api.config.Actionable
import appeng.api.implementations.blockentities.IWirelessAccessPoint
import appeng.api.networking.security.IActionSource
import net.minecraft.core.GlobalPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.server.ServerLifecycleHooks
import rhx.lazy.MOD_ID
import rhx.lazy.core.io.ConfigurationCardRegistries
import rhx.lazy.core.io.ConfigurationCardSources
import rhx.lazy.core.io.NetworkOutputProvider
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTargetResolution
import rhx.lazy.core.io.TransferResult
import rhx.lazy.core.resource.ResourceAmount
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceVariant

internal object Ae2NetworkOutputProvider : NetworkOutputProvider {
    val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, "ae2")

    override val id: ResourceLocation = ID
    override val displayName: Component = Component.translatable("gui.lazy.io.provider.ae2")
    override val capabilities: Set<ResourceKind<out ResourceVariant>>
        get() = AeStoragePayloadAdapters.capabilities

    override fun icon(): ItemStack =
        ItemStack(
            BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("ae2", "wireless_access_point"),
            ),
        )

    override fun resolvePrimaryTarget(player: ServerPlayer): NetworkTargetResolution {
        val orderedHands =
            listOf(
                player.getItemInHand(InteractionHand.MAIN_HAND),
                player.getItemInHand(InteractionHand.OFF_HAND),
            )
        val cards = ConfigurationCardSources.allCards(player)
        val inventoryTargets = cards.mapNotNull(::linkedTarget)
        return when (val selection = ConfigurationCardTargetSelection.select(orderedHands.map(::linkedTarget), inventoryTargets)) {
            is ConfigurationCardTargetSelection.Selected -> NetworkTargetResolution.Success(createTarget(selection.target))
            ConfigurationCardTargetSelection.Missing ->
                if (orderedHands.any(ConfigurationCardRegistries::isCard) || cards.isNotEmpty()) {
                    NetworkTargetResolution.Unlinked
                } else {
                    NetworkTargetResolution.NotFound
                }
            ConfigurationCardTargetSelection.Ambiguous -> NetworkTargetResolution.Ambiguous
        }
    }

    override fun isTargetValid(target: NetworkTargetRef): Boolean = Ae2NetworkTarget.parse(target) != null

    override fun offer(
        target: NetworkTargetRef,
        amount: ResourceAmount<out ResourceVariant>,
        simulate: Boolean,
    ): TransferResult {
        val globalPos = Ae2NetworkTarget.parse(target) ?: return TransferResult.InvalidTarget
        val converted = AeStoragePayloadAdapters.convert(amount) ?: return TransferResult.TemporarilyUnavailable

        return try {
            val server = ServerLifecycleHooks.getCurrentServer() ?: return TransferResult.TemporarilyUnavailable
            val level = server.getLevel(globalPos.dimension) ?: return TransferResult.TemporarilyUnavailable
            val chunkX = globalPos.pos.x shr 4
            val chunkZ = globalPos.pos.z shr 4
            if (level.chunkSource.getChunkNow(chunkX, chunkZ) == null) {
                return TransferResult.TemporarilyUnavailable
            }
            val accessPoint =
                level.getBlockEntity(globalPos.pos) as? IWirelessAccessPoint
                    ?: return TransferResult.TemporarilyUnavailable
            if (!accessPoint.isActive) return TransferResult.TemporarilyUnavailable
            val grid = accessPoint.grid ?: return TransferResult.TemporarilyUnavailable
            val inserted =
                grid.storageService.inventory.insert(
                    converted.key,
                    converted.amount,
                    Actionable.ofSimulate(simulate),
                    IActionSource.ofMachine(accessPoint),
                )
            TransferResult.Accepted(inserted.coerceIn(0L, converted.amount))
        } catch (_: LinkageError) {
            if (simulate) TransferResult.TemporarilyUnavailable else TransferResult.OutcomeUnknown
        } catch (_: RuntimeException) {
            if (simulate) TransferResult.TemporarilyUnavailable else TransferResult.OutcomeUnknown
        }
    }

    fun createTarget(pos: GlobalPos): NetworkTargetRef = Ae2NetworkTarget.create(pos)

    private fun linkedTarget(stack: ItemStack): GlobalPos? =
        stack.takeIf(ConfigurationCardRegistries::isCard)?.get(appeng.api.ids.AEComponents.WIRELESS_LINK_TARGET)
}
