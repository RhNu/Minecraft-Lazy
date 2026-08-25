package rhx.lazy.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files

public abstract class GenerateIntegrationDescriptor : DefaultTask() {
    @get:Input
    public abstract val integrationId: Property<String>

    @get:Input
    public abstract val owner: Property<String>

    @get:Input
    public abstract val side: Property<String>

    @get:Input
    public abstract val requiredMods: ListProperty<String>

    @get:Input
    public abstract val optionalMods: ListProperty<String>

    @get:Input
    public abstract val integrationDependencies: ListProperty<String>

    @get:Input
    public abstract val mixinConfigs: ListProperty<String>

    @get:Input
    public abstract val dataGen: Property<Boolean>

    @get:OutputFile
    public abstract val outputFile: RegularFileProperty

    @TaskAction
    public fun generate() {
        val json =
            buildString {
                appendLine("{")
                appendLine("  \"id\": \"${integrationId.get()}\",")
                appendLine("  \"owner\": \"${owner.get()}\",")
                appendLine("  \"side\": \"${side.get()}\",")
                appendLine("  \"requiredMods\": ${requiredMods.get().toJsonArray()},")
                appendLine("  \"optionalMods\": ${optionalMods.get().toJsonArray()},")
                appendLine("  \"dependencies\": ${integrationDependencies.get().toJsonArray()},")
                appendLine("  \"mixinConfigs\": ${mixinConfigs.get().toJsonArray()},")
                appendLine("  \"dataGen\": ${dataGen.get()}")
                appendLine("}")
            }
        val output = outputFile.get().asFile.toPath()
        Files.createDirectories(output.parent)
        Files.writeString(output, json)
    }

    private fun List<String>.toJsonArray(): String = joinToString(prefix = "[", postfix = "]") { value -> "\"$value\"" }
}

public abstract class ValidateIntegrationManifest : DefaultTask() {
    @get:Input
    public abstract val expectedRecords: ListProperty<String>

    @get:InputFile
    public abstract val manifestFile: RegularFileProperty

    @TaskAction
    public fun validate() {
        val linePattern = Regex("""\{"id":"([^"]+)","owner":"([^"]+)","side":"([^"]+)","requiredMods":\[(.*?)]""")
        val quotedValue = Regex(""""([^"]+)"""")
        val actual =
            linePattern.findAll(manifestFile.get().asFile.readText()).associate { match ->
                val requiredMods = quotedValue.findAll(match.groupValues[4]).map { it.groupValues[1] }.sorted().toList()
                match.groupValues[1] to listOf(match.groupValues[2], match.groupValues[3], requiredMods.joinToString(","))
            }
        val expected =
            expectedRecords.get().associate { record ->
                val fields = record.split('~')
                require(fields.size == 7) { "Malformed integration catalog record: $record" }
                val requiredMods = fields[3].split(',').filter(String::isNotBlank).sorted().joinToString(",")
                fields[0] to listOf(fields[1], fields[2], requiredMods)
            }
        check(actual == expected) {
            "KSP integration manifest does not match Gradle DSL descriptors. Expected $expected, got $actual"
        }
    }
}
