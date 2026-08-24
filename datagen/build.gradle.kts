import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.language.jvm.tasks.ProcessResources
import org.slf4j.event.Level

plugins {
    id("lazy.datagen")
}

description = "Development-only data generators for Lazy and its integrations."

val modId = providers.gradleProperty("mod_id").get()
val integrationApiProject = project(":integration-api")
val runtimeProject = project(":runtime")
val curiosProject = project(":integrations:curios")
val mysticalAgricultureProject = project(":integrations:mysticalagriculture")
val modProject = project(":mod")
val integrationProjects = project(":integrations").subprojects.sortedBy { integrationProject -> integrationProject.path }

(listOf(integrationApiProject, runtimeProject, modProject) + integrationProjects).forEach { dataGenProject ->
    evaluationDependsOn(dataGenProject.path)
}

val generateModMetadata = modProject.tasks.named<ProcessResources>("generateModMetadata")
sourceSets.main {
    resources.srcDir(generateModMetadata)
    resources.srcDir(modProject.layout.projectDirectory.dir("src/main/resources"))
}

extensions.configure<KspExtension> {
    val dataGenIntegrations =
        integrationProjects
            .map { integrationProject ->
                integrationProject.extensions.extraProperties.get("lazy.integration.catalogRecord").toString().split('~')
            }.filter { fields -> fields[5].toBooleanStrict() }
            .map { fields -> fields[0] }
            .plus("runtime")
            .sorted()
    arg("lazy.datagen.integrations", dataGenIntegrations.joinToString(","))
}

dependencies {
    implementation(runtimeProject)
    implementation(curiosProject)
    implementation(mysticalAgricultureProject)
    compileOnly(project(":codegen:integration-annotations"))
    ksp(project(":codegen:integration-processor"))
    implementation(libs.curios.runtime)
    runtimeOnly(libs.guideme.api)
    runtimeOnly(libs.cucumber)
    runtimeOnly(libs.mystical.agriculture)
}

neoForge {
    runs {
        create("data") {
            data()
            programArguments.addAll(
                "--mod",
                modId,
                "--all",
                "--output",
                modProject.layout.projectDirectory.dir("src/generated/resources").asFile.absolutePath,
                "--existing",
                modProject.layout.projectDirectory.dir("src/main/resources").asFile.absolutePath,
            )
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = Level.INFO
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(integrationApiProject.sourceSets.main.get())
            sourceSet(runtimeProject.sourceSets.main.get())
            sourceSet(curiosProject.sourceSets.main.get())
            sourceSet(mysticalAgricultureProject.sourceSets.main.get())
        }
    }
}
