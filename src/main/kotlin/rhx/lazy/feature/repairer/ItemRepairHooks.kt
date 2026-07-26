package rhx.lazy.feature.repairer

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID

internal sealed interface ItemRepairHookResult {
    data object Success : ItemRepairHookResult

    data object Failed : ItemRepairHookResult
}

internal fun interface ItemRepairHook {
    fun afterRepair(
        stack: ItemStack,
        player: Player?,
    ): ItemRepairHookResult
}

internal open class ItemRepairHookRegistry : ItemRepairHook {
    private val logger = LogManager.getLogger("$MOD_ID/ItemRepairHooks")
    private val handlers = mutableListOf<ItemRepairHook>()

    fun register(handler: ItemRepairHook) {
        check(handlers.none { registered -> registered === handler }) {
            "Item repair hook is already registered: ${handler.javaClass.name}"
        }
        handlers += handler
    }

    override fun afterRepair(
        stack: ItemStack,
        player: Player?,
    ): ItemRepairHookResult {
        var result: ItemRepairHookResult = ItemRepairHookResult.Success
        handlers.forEach { handler ->
            try {
                if (handler.afterRepair(stack, player) === ItemRepairHookResult.Failed) {
                    result = ItemRepairHookResult.Failed
                }
            } catch (error: LinkageError) {
                logger.error("Item repair hook linkage failed for {}", stack.hoverName.string, error)
                result = ItemRepairHookResult.Failed
            } catch (exception: RuntimeException) {
                logger.error("Item repair hook failed for {}", stack.hoverName.string, exception)
                result = ItemRepairHookResult.Failed
            }
        }
        return result
    }
}

internal object ItemRepairHooks : ItemRepairHookRegistry()
