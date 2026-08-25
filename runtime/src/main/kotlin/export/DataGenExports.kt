package rhx.lazy.export

import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.world.item.Item
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.dimension.LevelStem
import rhx.lazy.core.configurator.ModularConfiguratorRegistries
import rhx.lazy.core.io.ConfigurationCardRegistries
import rhx.lazy.feature.buffer.BufferRegistries
import rhx.lazy.feature.energy.EnergyRegistries
import rhx.lazy.feature.machine.MachineCasingRegistries
import rhx.lazy.feature.machine.ProcessingCoreRegistries
import rhx.lazy.feature.repairer.RepairerRegistries
import rhx.lazy.feature.replicator.ReplicatorRegistries
import rhx.lazy.feature.shaping.ShaperRegistries
import rhx.lazy.feature.simulation.SimulationRecipeData
import rhx.lazy.feature.simulation.SimulationRegistries
import rhx.lazy.feature.teleporter.TeleporterRegistries
import rhx.lazy.feature.voidworld.VoidWorldBootstrap
import rhx.lazy.feature.voidworld.VoidWorldRegistries
import rhx.lazy.integration.api.LazyInternalApi

/** Narrow execution-time view of runtime registry holders used by the standalone DataGen project. */
@LazyInternalApi
public object DataGenExports {
    public fun machineCasingBlock(): Block = MachineCasingRegistries.block.get()

    public fun machineCasingItem(): Item = MachineCasingRegistries.item.get()

    public fun configurationCardItem(): Item = ConfigurationCardRegistries.item.get()

    public fun modularConfiguratorItem(): Item = ModularConfiguratorRegistries.item.get()

    public fun bufferBlock(): Block = BufferRegistries.block.get()

    public fun bufferItem(): Item = BufferRegistries.item.get()

    public fun energyBatteryItem(): Item = EnergyRegistries.batteryItem.get()

    public fun energySourceBlock(): Block = EnergyRegistries.sourceBlock.get()

    public fun energySourceItem(): Item = EnergyRegistries.sourceItem.get()

    public fun replicatorBlock(): Block = ReplicatorRegistries.block.get()

    public fun replicatorItem(): Item = ReplicatorRegistries.item.get()

    public fun repairerBlock(): Block = RepairerRegistries.block.get()

    public fun repairerItem(): Item = RepairerRegistries.item.get()

    public fun shaperBlock(): Block = ShaperRegistries.block.get()

    public fun shaperItem(): Item = ShaperRegistries.item.get()

    public fun simulationBlock(): Block = SimulationRegistries.block.get()

    public fun simulationItem(): Item = SimulationRegistries.item.get()

    public fun dataModelItem(): Item = SimulationRegistries.dataModelItem.get()

    public fun teleporterItem(): Item = TeleporterRegistries.item.get()

    public fun encapsulatedSpaceWallBlock(): Block = VoidWorldRegistries.spaceWall.get()

    public fun encapsulatedSpaceFrameBlock(): Block = VoidWorldRegistries.spaceFrame.get()

    public fun processingCoreT1(): Item = ProcessingCoreRegistries.t1.get()

    public fun processingCoreT2(): Item = ProcessingCoreRegistries.t2.get()

    public fun processingCoreT3(): Item = ProcessingCoreRegistries.t3.get()

    public fun processingCoreT4(): Item = ProcessingCoreRegistries.t4.get()

    public fun bootstrapVoidBiome(context: BootstrapContext<Biome>) {
        VoidWorldBootstrap.bootstrapBiome(context)
    }

    public fun bootstrapVoidDimensionType(context: BootstrapContext<DimensionType>) {
        VoidWorldBootstrap.bootstrapDimensionType(context)
    }

    public fun bootstrapVoidLevelStem(context: BootstrapContext<LevelStem>) {
        VoidWorldBootstrap.bootstrapLevelStem(context)
    }

    public fun buildSimulationRecipes(output: RecipeOutput) {
        SimulationRecipeData.build(output)
    }
}
