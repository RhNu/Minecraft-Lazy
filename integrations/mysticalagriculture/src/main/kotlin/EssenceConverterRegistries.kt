package rhx.lazy.integration.mysticalagriculture

import net.minecraft.core.registries.Registries
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.registry.RegistryModule
import rhx.lazy.core.registry.buildType
import rhx.lazy.core.registry.registerBlockItem
import java.util.function.Supplier

internal object EssenceConverterRegistries : RegistryModule {
    private val blocks = DeferredRegister.createBlocks(MOD_ID)
    private val items = DeferredRegister.createItems(MOD_ID)
    private val blockEntities: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID)
    private val recipeSerializers: DeferredRegister<RecipeSerializer<*>> =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID)

    val block = blocks.register("essence_converter", Supplier(::EssenceConverterBlock))
    val item =
        items.registerBlockItem(
            "essence_converter",
            block,
        ) { registeredBlock, properties -> EssenceConverterBlockItem(registeredBlock, properties) }
    val blockEntity =
        blockEntities.register(
            "essence_converter",
            Supplier {
                BlockEntityType.Builder
                    .of(::EssenceConverterBlockEntity, block.get())
                    .buildType()
            },
        )
    val consumingShapedRecipe =
        recipeSerializers.register(
            "consuming_shaped",
            Supplier { ConsumingShapedRecipe.Serializer() },
        )

    override fun register(bus: IEventBus) {
        blocks.register(bus)
        items.register(bus)
        blockEntities.register(bus)
        recipeSerializers.register(bus)
    }
}
