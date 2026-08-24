package rhx.lazy.buildlogic

import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files

public abstract class RenderSvgTextures : DefaultTask() {
    @get:InputDirectory
    public abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    public abstract val textureDirectory: DirectoryProperty

    @TaskAction
    public fun render() {
        val sourceRoot = sourceDirectory.get().asFile.toPath()
        val textureRoot = textureDirectory.get().asFile.toPath()
        val svgFiles =
            Files.walk(sourceRoot).use { stream ->
                stream.filter { path -> Files.isRegularFile(path) && path.toString().endsWith(".svg") }.sorted().toList()
            }
        require(svgFiles.isNotEmpty()) { "No SVG files were found under $sourceRoot" }
        svgFiles.forEach { svgFile ->
            val relativePath = sourceRoot.relativize(svgFile)
            val pngFile = textureRoot.resolve(relativePath.parent).resolve("${svgFile.fileName.toString().removeSuffix(".svg")}.png")
            Files.createDirectories(pngFile.parent)
            Files.newInputStream(svgFile).use { inputStream ->
                Files.newOutputStream(pngFile).use { outputStream ->
                    PNGTranscoder().apply {
                        addTranscodingHint(PNGTranscoder.KEY_WIDTH, 16f)
                        addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 16f)
                    }.transcode(TranscoderInput(inputStream), TranscoderOutput(outputStream))
                }
            }
        }
    }
}
