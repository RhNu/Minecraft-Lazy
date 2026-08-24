package rhx.lazy.core.configurator

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import org.apache.logging.log4j.LogManager
import rhx.lazy.MOD_ID
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public interface ModularConfiguratorModule {
    val id: ResourceLocation

    fun acceptsMaterial(stack: ItemStack): Boolean

    /** Returns null when this module does not apply and the configurator should continue dispatching. */
    fun useOn(context: UseOnContext): InteractionResult?
}

internal class ModularConfiguratorModuleRegistry {
    private val logger = LogManager.getLogger("$MOD_ID/ModularConfiguratorModules")
    private val modules = linkedMapOf<ResourceLocation, ModularConfiguratorModule>()

    @Synchronized
    fun register(module: ModularConfiguratorModule) {
        require(modules.putIfAbsent(module.id, module) == null) {
            "Duplicate modular configurator module ${module.id}"
        }
    }

    fun acceptsMaterial(stack: ItemStack): Boolean =
        !stack.isEmpty &&
            snapshot().any { module ->
                try {
                    module.acceptsMaterial(stack)
                } catch (error: LinkageError) {
                    logger.error("Configurator material check failed in module {}", module.id, error)
                    false
                } catch (exception: RuntimeException) {
                    logger.error("Configurator material check failed in module {}", module.id, exception)
                    false
                }
            }

    fun useOn(context: UseOnContext): InteractionResult? {
        snapshot().forEach { module ->
            val result =
                try {
                    module.useOn(context)
                } catch (error: LinkageError) {
                    logger.error("Configurator interaction failed in module {}", module.id, error)
                    InteractionResult.FAIL
                } catch (exception: RuntimeException) {
                    logger.error("Configurator interaction failed in module {}", module.id, exception)
                    InteractionResult.FAIL
                }
            if (result != null) return result
        }
        return null
    }

    @Synchronized
    fun snapshot(): List<ModularConfiguratorModule> = modules.values.toList()
}

@LazyInternalApi
public object ModularConfiguratorModules {
    private val registry = ModularConfiguratorModuleRegistry()

    public fun register(module: ModularConfiguratorModule) = registry.register(module)

    internal fun acceptsMaterial(stack: ItemStack): Boolean = registry.acceptsMaterial(stack)

    internal fun useOn(context: UseOnContext): InteractionResult? = registry.useOn(context)

    internal fun snapshot(): List<ModularConfiguratorModule> = registry.snapshot()
}
