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
import rhx.lazy.core.io.NetworkInsertCapability
import rhx.lazy.core.io.NetworkOutputProvider
import rhx.lazy.core.io.NetworkPayload
import rhx.lazy.core.io.NetworkTargetRef
import rhx.lazy.core.io.NetworkTargetResolution
import rhx.lazy.core.io.NetworkTransferResult

internal object Ae2NetworkOutputProvider : NetworkOutputProvider {
    val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, "ae2")

    override val id: ResourceLocation = ID
    override val displayName: Component = Component.translatable("gui.lazy.io.provider.ae2")
    override val capabilities: Set<NetworkInsertCapability>
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
        val curiosCard =
            if (CuriosLinkCardSource.isAvailable()) {
                CuriosLinkCardSource.findEquippedCard(player)
            } else {
                null
            }
        val inventoryTargets =
            player.inventory.items.mapNotNull(::linkedTarget) +
                listOfNotNull(curiosCard).mapNotNull(::linkedTarget)
        return when (val selection = MeLinkCardSelection.select(orderedHands.map(::linkedTarget), inventoryTargets)) {
            is MeLinkCardSelection.Selected -> NetworkTargetResolution.Success(createTarget(selection.target))
            MeLinkCardSelection.Missing ->
                if (orderedHands.any(Ae2Registries::isLinkCard) ||
                    player.inventory.items.any(Ae2Registries::isLinkCard) ||
                    listOfNotNull(curiosCard).any(Ae2Registries::isLinkCard)
                ) {
                    NetworkTargetResolution.Unlinked
                } else {
                    NetworkTargetResolution.NotFound
                }
            MeLinkCardSelection.Ambiguous -> NetworkTargetResolution.Ambiguous
        }
    }

    override fun isTargetValid(target: NetworkTargetRef): Boolean = Ae2NetworkTarget.parse(target) != null

    override fun insert(
        target: NetworkTargetRef,
        payload: NetworkPayload,
        simulate: Boolean,
    ): NetworkTransferResult {
        val globalPos = Ae2NetworkTarget.parse(target) ?: return NetworkTransferResult.InvalidTarget
        val converted = AeStoragePayloadAdapters.convert(payload) ?: return NetworkTransferResult.TemporarilyUnavailable
        if (converted.amount <= 0) return NetworkTransferResult.Success(0)

        return try {
            val server = ServerLifecycleHooks.getCurrentServer() ?: return NetworkTransferResult.TemporarilyUnavailable
            val level = server.getLevel(globalPos.dimension) ?: return NetworkTransferResult.TemporarilyUnavailable
            val chunkX = globalPos.pos.x shr 4
            val chunkZ = globalPos.pos.z shr 4
            if (level.chunkSource.getChunkNow(chunkX, chunkZ) == null) {
                return NetworkTransferResult.TemporarilyUnavailable
            }
            val accessPoint =
                level.getBlockEntity(globalPos.pos) as? IWirelessAccessPoint
                    ?: return NetworkTransferResult.TemporarilyUnavailable
            if (!accessPoint.isActive) return NetworkTransferResult.TemporarilyUnavailable
            val grid = accessPoint.grid ?: return NetworkTransferResult.TemporarilyUnavailable
            val inserted =
                grid.storageService.inventory.insert(
                    converted.key,
                    converted.amount,
                    Actionable.ofSimulate(simulate),
                    IActionSource.ofMachine(accessPoint),
                )
            NetworkTransferResult.Success((converted.amount - inserted).coerceAtLeast(0))
        } catch (_: LinkageError) {
            if (simulate) NetworkTransferResult.TemporarilyUnavailable else NetworkTransferResult.OutcomeUnknown
        } catch (_: RuntimeException) {
            if (simulate) NetworkTransferResult.TemporarilyUnavailable else NetworkTransferResult.OutcomeUnknown
        }
    }

    fun createTarget(pos: GlobalPos): NetworkTargetRef = Ae2NetworkTarget.create(pos)

    private fun linkedTarget(stack: ItemStack): GlobalPos? =
        stack.takeIf(Ae2Registries::isLinkCard)?.get(appeng.api.ids.AEComponents.WIRELESS_LINK_TARGET)
}
