package rhx.lazy.feature.simulation

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import rhx.lazy.MOD_ID
import rhx.lazy.core.lazyId
import rhx.lazy.core.registry.RegistryModule
import rhx.lazy.core.registry.buildType
import rhx.lazy.core.registry.registerBlockItem
import java.util.function.Supplier

internal object SimulationRegistries : RegistryModule {
    private val blocks = DeferredRegister.createBlocks(MOD_ID)
    private val items = DeferredRegister.createItems(MOD_ID)
    private val blockEntities: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID)
    private val recipeTypes: DeferredRegister<RecipeType<*>> =
        DeferredRegister.create(Registries.RECIPE_TYPE, MOD_ID)
    private val recipeSerializers: DeferredRegister<RecipeSerializer<*>> =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID)
    private val dataComponents: DeferredRegister.DataComponents =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID)

    val block = blocks.register("simulation_chamber", Supplier(::SimulationChamberBlock))
    val item =
        items.registerBlockItem("simulation_chamber", block) { registeredBlock, properties ->
            SimulationChamberBlockItem(registeredBlock, properties)
        }
    val dataModelItem =
        items.register(
            "data_model",
            Supplier { DataModelItem(Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)) },
        )
    val coreT1 = core("simulation_core_t1", SimulationCoreTier.T1)
    val coreT2 = core("simulation_core_t2", SimulationCoreTier.T2)
    val coreT3 = core("simulation_core_t3", SimulationCoreTier.T3)
    val coreT4 = core("simulation_core_t4", SimulationCoreTier.T4)

    val blockEntity =
        blockEntities.register(
            "simulation_chamber",
            Supplier {
                BlockEntityType.Builder
                    .of({ pos, state -> SimulationChamberBlockEntity(pos, state) }, block.get())
                    .buildType()
            },
        )

    val itemRecipeType =
        recipeTypes.register("item_simulation", Supplier { RecipeType.simple<ItemSimulationRecipe>(ITEM_RECIPE_ID) })
    val entityRecipeType =
        recipeTypes.register("entity_simulation", Supplier { RecipeType.simple<EntitySimulationRecipe>(ENTITY_RECIPE_ID) })
    val itemInjectionRecipeType =
        recipeTypes.register(
            "item_simulation_injection",
            Supplier { RecipeType.simple<ItemSimulationInjectionRecipe>(ITEM_INJECTION_RECIPE_ID) },
        )
    val itemRecipeSerializer =
        recipeSerializers.register("item_simulation", Supplier { ItemSimulationRecipe.Serializer() })
    val entityRecipeSerializer =
        recipeSerializers.register("entity_simulation", Supplier { EntitySimulationRecipe.Serializer() })
    val itemInjectionRecipeSerializer =
        recipeSerializers.register("item_simulation_injection", Supplier { ItemSimulationInjectionRecipe.Serializer() })

    val entityTypeComponent =
        dataComponents.registerComponentType("data_model_entity") { builder ->
            builder
                .persistent(ResourceLocation.CODEC)
                .networkSynchronized(ResourceLocation.STREAM_CODEC)
        }

    fun coreTier(stack: net.minecraft.world.item.ItemStack): SimulationCoreTier? = (stack.item as? SimulationCoreItem)?.tier

    fun allCoreItems(): List<Supplier<out Item>> = listOf(coreT1, coreT2, coreT3, coreT4)

    override fun register(bus: IEventBus) {
        blocks.register(bus)
        items.register(bus)
        blockEntities.register(bus)
        recipeTypes.register(bus)
        recipeSerializers.register(bus)
        dataComponents.register(bus)
    }

    private fun core(
        name: String,
        tier: SimulationCoreTier,
    ) = items.register(name, Supplier { SimulationCoreItem(tier, Item.Properties().stacksTo(64)) })

    private val ITEM_RECIPE_ID = lazyId("item_simulation")
    private val ENTITY_RECIPE_ID = lazyId("entity_simulation")
    private val ITEM_INJECTION_RECIPE_ID = lazyId("item_simulation_injection")
}
