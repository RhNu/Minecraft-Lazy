package rhx.lazy.core.resource

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import rhx.lazy.integration.api.LazyInternalApi

/** Registry-aware wire form shared by screens and integrations that transport abstract resources. */
@LazyInternalApi
public object ResourceAmountStreamCodec :
    StreamCodec<RegistryFriendlyByteBuf, ResourceAmount<out ResourceVariant>> {
    override fun encode(
        buffer: RegistryFriendlyByteBuf,
        value: ResourceAmount<out ResourceVariant>,
    ) {
        buffer.writeNbt(value.save(buffer.registryAccess()))
    }

    override fun decode(buffer: RegistryFriendlyByteBuf): ResourceAmount<out ResourceVariant> =
        requireNotNull(buffer.readNbt()?.let { ResourceAmount.parse(buffer.registryAccess(), it) }) {
            "Invalid abstract resource amount payload"
        }
}
