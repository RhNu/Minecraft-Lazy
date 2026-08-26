package rhx.lazy.integration.mekanism

import mekanism.api.Action
import mekanism.api.chemical.ChemicalStack
import mekanism.api.chemical.IChemicalHandler
import mekanism.common.capabilities.Capabilities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.BlockCapabilityCache
import rhx.lazy.core.io.ResourceFaceTransfer
import rhx.lazy.core.io.ResourceFaceTransferFactories
import rhx.lazy.core.io.ResourceFaceTransferFactory
import rhx.lazy.core.resource.ResourceContainerExtractor
import rhx.lazy.core.resource.ResourceContainerExtractors
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceKinds
import rhx.lazy.core.resource.ResourceSprite
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.integration.api.LazyInternalApi
import java.util.EnumMap
import java.util.function.BooleanSupplier

@LazyInternalApi
public class ChemicalVariant private constructor(
    stack: ChemicalStack,
) : ResourceVariant {
    private val storedTemplate = stack.copyWithAmount(1L)

    val template: ChemicalStack
        get() = storedTemplate.copy()

    fun matches(other: ChemicalVariant): Boolean = ChemicalStack.isSameChemical(storedTemplate, other.storedTemplate)

    override fun copyVariant(): ChemicalVariant = ChemicalVariant(storedTemplate)

    override fun toString(): String = storedTemplate.toString()

    companion object {
        fun of(stack: ChemicalStack): ChemicalVariant? = stack.takeUnless(ChemicalStack::isEmpty)?.let(::ChemicalVariant)
    }
}

@LazyInternalApi
public object ChemicalResourceKind : ResourceKind<ChemicalVariant> {
    override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath("mekanism", "chemical")
    override val displayName: Component = Component.translatable("gui.lazy.io.capability.mekanism_chemical")
    override val defaultAmount: Long = 1_000L

    override fun variantName(variant: ChemicalVariant): Component = variant.template.textComponent

    override fun sprite(variant: ChemicalVariant): ResourceSprite {
        val template = variant.template
        return ResourceSprite(template.chemical.icon, template.chemicalTint.withOpaqueAlpha())
    }

    override fun matches(
        first: ChemicalVariant,
        second: ChemicalVariant,
    ): Boolean = first.matches(second)

    override fun copy(variant: ChemicalVariant): ChemicalVariant = variant.copyVariant()

    override fun save(
        registries: HolderLookup.Provider,
        variant: ChemicalVariant,
    ): CompoundTag = variant.template.save(registries) as? CompoundTag ?: CompoundTag()

    override fun parse(
        registries: HolderLookup.Provider,
        tag: CompoundTag,
    ): ChemicalVariant? = ChemicalVariant.of(ChemicalStack.parseOptional(registries, tag))

    private fun Int.withOpaqueAlpha(): Int = if (this ushr 24 == 0) this or -0x1000000 else this
}

internal object ChemicalFaceTransferFactory : ResourceFaceTransferFactory<ChemicalVariant> {
    override val kind = ChemicalResourceKind

    override fun create(
        origin: BlockPos,
        stillValid: BooleanSupplier,
    ): ResourceFaceTransfer<ChemicalVariant> = ChemicalFaceTransfer(origin, stillValid)
}

internal object ChemicalContainerExtractor : ResourceContainerExtractor<ChemicalVariant> {
    override val kind: ResourceKind<ChemicalVariant> = ChemicalResourceKind

    override fun extract(stack: ItemStack): ChemicalVariant? {
        val handler = Capabilities.CHEMICAL.getCapability(stack) ?: return null
        return (0 until handler.chemicalTanks)
            .firstNotNullOfOrNull { tank -> ChemicalVariant.of(handler.getChemicalInTank(tank)) }
    }
}

private class ChemicalFaceTransfer(
    private val origin: BlockPos,
    private val stillValid: BooleanSupplier,
) : ResourceFaceTransfer<ChemicalVariant> {
    private val targets = EnumMap<Direction, BlockCapabilityCache<IChemicalHandler, Direction?>>(Direction::class.java)

    override fun offer(
        level: ServerLevel,
        direction: Direction,
        variant: ChemicalVariant,
        amount: Long,
    ): Long {
        if (amount <= 0L) return 0L
        val target =
            targets
                .getOrPut(direction) {
                    Capabilities.CHEMICAL.createCache(
                        level,
                        origin.relative(direction),
                        direction.opposite,
                        stillValid,
                        {},
                    )
                }.capability
                ?: return 0L
        val remainder = target.insertChemical(variant.template.copyWithAmount(amount), Action.EXECUTE)
        return amount - remainder.amount.coerceIn(0L, amount)
    }

    override fun invalidate() {
        targets.clear()
    }
}

internal object MekanismChemicalResourceIntegration {
    fun install() {
        ResourceKinds.register(ChemicalResourceKind)
        ResourceFaceTransferFactories.register(ChemicalFaceTransferFactory)
        ResourceContainerExtractors.register(ChemicalContainerExtractor)
    }
}
