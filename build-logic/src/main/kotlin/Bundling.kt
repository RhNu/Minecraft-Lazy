package rhx.lazy.buildlogic

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import javax.inject.Inject

public abstract class UnpackBundledModule : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    public abstract val inputArtifact: Provider<FileSystemLocation>

    @get:Inject
    public abstract val archiveOperations: ArchiveOperations

    @get:Inject
    public abstract val fileSystemOperations: FileSystemOperations

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val output = outputs.dir(input.nameWithoutExtension)
        fileSystemOperations.sync {
            from(archiveOperations.zipTree(input))
            into(output)
            exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
        }
    }
}
