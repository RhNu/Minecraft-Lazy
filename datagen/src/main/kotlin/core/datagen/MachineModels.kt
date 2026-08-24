package rhx.lazy.core.datagen

import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder
import net.neoforged.neoforge.client.model.generators.BlockModelProvider
import net.neoforged.neoforge.client.model.generators.ModelFile

internal fun BlockModelProvider.orientableMachineModel(
    name: String,
    bottom: ResourceLocation,
    side: ResourceLocation,
    top: ResourceLocation,
    overlay: ResourceLocation,
    topOverlay: ResourceLocation? = null,
): BlockModelBuilder {
    val builder =
        getBuilder(name)
            .parent(ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("block/block")))
            .texture("particle", "#side")
            .texture("bottom", bottom)
            .texture("side", side)
            .texture("top", top)
            .texture("overlay", overlay)
            .renderType("cutout")

    if (topOverlay != null) {
        builder.texture("top_overlay", topOverlay)
    }

    builder
        .element()
        .from(0f, 0f, 0f)
        .to(16f, 16f, 16f)
        .face(Direction.DOWN)
        .uvs(0f, 0f, 16f, 16f)
        .texture("#bottom")
        .cullface(Direction.DOWN)
        .end()
        .face(Direction.UP)
        .uvs(0f, 0f, 16f, 16f)
        .texture("#top")
        .cullface(Direction.UP)
        .end()
        .face(Direction.NORTH)
        .uvs(0f, 0f, 16f, 16f)
        .texture("#bottom")
        .cullface(Direction.NORTH)
        .end()
        .face(Direction.SOUTH)
        .uvs(0f, 0f, 16f, 16f)
        .texture("#side")
        .cullface(Direction.SOUTH)
        .end()
        .face(Direction.WEST)
        .uvs(0f, 0f, 16f, 16f)
        .texture("#side")
        .cullface(Direction.WEST)
        .end()
        .face(Direction.EAST)
        .uvs(0f, 0f, 16f, 16f)
        .texture("#side")
        .cullface(Direction.EAST)
        .end()
        .end()

    val overlayToY = if (topOverlay != null) 16.01f else 16f
    val overlayElement =
        builder
            .element()
            .from(0f, 0f, -0.01f)
            .to(16f, overlayToY, 16f)

    overlayElement
        .face(Direction.NORTH)
        .uvs(0f, 0f, 16f, 16f)
        .texture("#overlay")
        .cullface(Direction.NORTH)
        .end()

    if (topOverlay != null) {
        overlayElement
            .face(Direction.UP)
            .uvs(0f, 0f, 16f, 16f)
            .texture("#top_overlay")
            .cullface(Direction.UP)
            .end()
    }

    overlayElement.end()

    return builder
}
