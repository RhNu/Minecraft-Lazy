package rhx.lazy.feature.machine

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import java.util.function.Supplier

internal object ProcessingCoreRegistries : RegistryModule {
    private val items = DeferredRegister.createItems(MOD_ID)

    val t1 = core("processing_core_t1", ProcessingCoreTier.T1)
    val t2 = core("processing_core_t2", ProcessingCoreTier.T2)
    val t3 = core("processing_core_t3", ProcessingCoreTier.T3)
    val t4 = core("processing_core_t4", ProcessingCoreTier.T4)

    fun tier(stack: ItemStack): ProcessingCoreTier? = (stack.item as? ProcessingCoreItem)?.tier

    fun allItems(): List<Supplier<out Item>> = listOf(t1, t2, t3, t4)

    override fun register(bus: IEventBus) {
        items.register(bus)
    }

    private fun core(
        name: String,
        tier: ProcessingCoreTier,
    ) = items.register(name, Supplier { ProcessingCoreItem(tier, Item.Properties().stacksTo(64)) })
}
