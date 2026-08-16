package rhx.lazy.integration.jade.client

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import rhx.lazy.core.io.IoMode
import rhx.lazy.core.io.NetworkOutputProviders
import rhx.lazy.integration.jade.IoMachineJadeData
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.StreamServerDataProvider
import snownee.jade.api.config.IPluginConfig

/** Common decode and UID plumbing shared by every machine component provider. */
internal abstract class MachineJadeComponentProvider<D>(
    private val dataProvider: StreamServerDataProvider<BlockAccessor, D>,
    private val uid: ResourceLocation,
) : IBlockComponentProvider {
    final override fun appendTooltip(
        tooltip: ITooltip,
        accessor: BlockAccessor,
        config: IPluginConfig,
    ) {
        val data = dataProvider.decodeFromData(accessor).orElse(null) ?: return
        appendTooltip(tooltip, data)
    }

    protected abstract fun appendTooltip(
        tooltip: ITooltip,
        data: D,
    )

    final override fun getUid(): ResourceLocation = uid
}

/**
 * IO-managed specialization that fixes the output row's placement and rendering while leaving
 * hooks on both sides for machine-specific content.
 */
internal abstract class IoMachineJadeComponentProvider<D : IoMachineJadeData>(
    dataProvider: StreamServerDataProvider<BlockAccessor, D>,
    uid: ResourceLocation,
) : MachineJadeComponentProvider<D>(dataProvider, uid) {
    final override fun appendTooltip(
        tooltip: ITooltip,
        data: D,
    ) {
        appendBeforeOutput(tooltip, data)
        tooltip.add(Component.translatable("jade.lazy.output", data.output.description()))
        appendAfterOutput(tooltip, data)
    }

    protected open fun appendBeforeOutput(
        tooltip: ITooltip,
        data: D,
    ) = Unit

    protected open fun appendAfterOutput(
        tooltip: ITooltip,
        data: D,
    ) = Unit
}

private fun rhx.lazy.integration.jade.JadeOutputState.description(): Component =
    when (mode) {
        IoMode.PASSIVE -> Component.translatable("jade.lazy.output.passive")
        IoMode.FACE ->
            Component.translatable(
                "jade.lazy.output.face",
                Component.translatable(if (autoEject) "jade.lazy.on" else "jade.lazy.off"),
            )
        IoMode.NETWORK ->
            networkProviderId
                ?.let(NetworkOutputProviders::get)
                ?.displayName
                ?: Component.translatable("jade.lazy.output.network")
    }
