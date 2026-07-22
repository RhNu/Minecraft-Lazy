package rhx.compose.menu

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.network.PacketDistributor
import rhx.compose.block.entity.BufferSnapshot
import rhx.compose.block.entity.BufferBlockEntity
import rhx.compose.network.BufferContentsPayload
import rhx.compose.registry.ModBlocks
import rhx.compose.registry.ModMenus

internal class BufferMenu private constructor(
    containerId: Int,
    private val access: ContainerLevelAccess,
    private val blockEntity: BufferBlockEntity?,
    private val serverPlayer: ServerPlayer?,
    initialSnapshot: BufferSnapshot,
) : AbstractContainerMenu(ModMenus.buffer.get(), containerId) {
    var snapshot: BufferSnapshot = initialSnapshot
        private set

    private var lastSentVersion: Long = blockEntity?.contentVersion ?: 0

    constructor(
        containerId: Int,
        _inventory: Inventory,
        extraData: RegistryFriendlyByteBuf,
    ) : this(
        containerId,
        ContainerLevelAccess.NULL,
        null,
        null,
        BufferSnapshot.read(extraData),
    )

    fun applyClientSnapshot(snapshot: BufferSnapshot) {
        this.snapshot = snapshot
    }

    fun clearContents(player: Player): Boolean {
        val serverPlayer = serverPlayer ?: return false
        val blockEntity = blockEntity ?: return false
        if (player !== serverPlayer || !stillValid(player)) return false
        return blockEntity.clearContents()
    }

    override fun broadcastChanges() {
        super.broadcastChanges()
        val blockEntity = blockEntity ?: return
        val serverPlayer = serverPlayer ?: return
        if (blockEntity.contentVersion != lastSentVersion) {
            snapshot = blockEntity.snapshot()
            lastSentVersion = blockEntity.contentVersion
            PacketDistributor.sendToPlayer(
                serverPlayer,
                BufferContentsPayload(containerId, snapshot),
            )
        }
    }

    override fun stillValid(player: Player): Boolean =
        stillValid(access, player, ModBlocks.buffer.get())

    override fun quickMoveStack(
        player: Player,
        index: Int,
    ): ItemStack = ItemStack.EMPTY

    companion object {
        fun createServer(
            containerId: Int,
            inventory: Inventory,
            blockEntity: BufferBlockEntity,
            player: ServerPlayer,
        ): BufferMenu =
            BufferMenu(
                containerId,
                ContainerLevelAccess.create(blockEntity.level ?: player.level(), blockEntity.blockPos),
                blockEntity,
                player,
                blockEntity.snapshot(),
            )
    }
}
