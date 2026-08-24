package rhx.lazy.buildlogic

import com.google.devtools.ksp.gradle.KspExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

public class LazyKotlinLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            configureLazyRepositories()
            pluginManager.apply("java-library")
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")

            val libraries = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

            extensions.configure<JavaPluginExtension> {
                toolchain.languageVersion.set(JavaLanguageVersion.of(21))
                withSourcesJar()
            }
            tasks.withType<KotlinCompile>().configureEach {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
            }
            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
                systemProperty("lazy.projectDir", rootProject.projectDir.absolutePath)
            }
            extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
                version.set(libraries.findVersion("ktlint").get().requiredVersion)
                outputToConsole.set(true)
                coloredOutput.set(true)
                filter {
                    exclude("**/generated/**")
                    exclude("**/build.gradle.kts")
                }
            }
            dependencies.add("compileOnly", libraries.findLibrary("kotlin-stdlib").get())
            dependencies.add("testImplementation", libraries.findLibrary("kotlin-stdlib").get())
            dependencies.add("testImplementation", libraries.findLibrary("kotlin-test").get())
            dependencies.add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }
    }
}

public class LazyNeoForgeLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("lazy.kotlin-library")
        project.pluginManager.apply("net.neoforged.moddev")
        project.extensions.configure<JavaPluginExtension> {
            sourceSets.named("test") {
                resources.srcDir(project.rootProject.file("mod/src/main/resources"))
                resources.srcDir(project.rootProject.file("mod/src/generated/resources"))
            }
        }
        project.tasks.named("processTestResources") {
            dependsOn(":mod:generateModMetadata")
        }
        project.extensions.configure<NeoForgeExtension> {
            version = project.providers.gradleProperty("neo_version").get()
            parchment.minecraftVersion.set(project.providers.gradleProperty("parchment_minecraft_version"))
            parchment.mappingsVersion.set(project.providers.gradleProperty("parchment_mappings_version"))
            unitTest.enable()
            if (project.path != ":mod") {
                val sourceSets = project.extensions.getByType(JavaPluginExtension::class.java).sourceSets
                val testMetadata = sourceSets.create("neoForgeTestMod") {
                    resources.srcDir(project.rootProject.file("gradle/testmod"))
                }
                val testedLibrary = mods.create("lazy_test") {
                    sourceSet(sourceSets.getByName("main"))
                    sourceSet(sourceSets.getByName("test"))
                    sourceSet(testMetadata)
                }
                unitTest.testedMod.set(testedLibrary)
            }
        }
    }
}

public class LazyIntegrationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("lazy.neoforge-library")
        project.pluginManager.apply("com.google.devtools.ksp")

        project.dependencies.add("implementation", project.dependencies.project(mapOf("path" to ":runtime")))
        project.dependencies.add(
            "compileOnly",
            project.dependencies.project(mapOf("path" to ":codegen:integration-annotations")),
        )
        project.dependencies.add(
            "ksp",
            project.dependencies.project(mapOf("path" to ":codegen:integration-processor")),
        )

        val integrationRuntime =
            project.configurations.create("integrationRuntime") {
                isCanBeConsumed = false
                isCanBeResolved = false
            }
        project.configurations.create("integrationRuntimeElements") {
            isCanBeConsumed = true
            isCanBeResolved = false
            extendsFrom(integrationRuntime)
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        }

        val extension = project.extensions.create("lazyIntegration", LazyIntegrationExtension::class.java)
        val descriptorTask =
            project.tasks.register<GenerateIntegrationDescriptor>("generateIntegrationDescriptor") {
                integrationId.set(extension.id)
                owner.set(extension.owner.map { value -> value.name })
                side.set(extension.side.map { value -> value.name })
                requiredMods.set(extension.requiredMods)
                optionalMods.set(extension.optionalMods)
                integrationDependencies.set(extension.integrationDependencies)
                dataGen.set(extension.dataGen)
                outputFile.set(project.layout.buildDirectory.file("integration/descriptor.json"))
            }

        val descriptorElements =
            project.configurations.create("integrationDescriptorElements") {
                isCanBeConsumed = true
                isCanBeResolved = false
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.DOCUMENTATION))
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named("lazy-integration-descriptor"))
            }
        project.artifacts.add(descriptorElements.name, descriptorTask.flatMap(GenerateIntegrationDescriptor::outputFile))
        project.tasks.named("assemble") {
            dependsOn(descriptorTask)
        }

        project.afterEvaluate {
            val integrationId = extension.id.get()
            require(integrationId.matches(Regex("[a-z0-9_.-]+"))) { "Invalid Lazy integration id: $integrationId" }
            val bridgeName =
                integrationId.split('-', '_', '.').joinToString("") { part -> part.replaceFirstChar(Char::uppercaseChar) }
            project.extensions.getByType(ExtraPropertiesExtension::class.java).set(
                "lazy.integration.catalogRecord",
                listOf(
                    integrationId,
                    extension.owner.get().name,
                    extension.side.get().name,
                    extension.requiredMods.get().joinToString(","),
                    extension.integrationDependencies.get().joinToString(","),
                    extension.dataGen.get().toString(),
                    "rhx.lazy.generated.integration.${integrationId.replace('-', '_').replace('.', '_')}." +
                        "${bridgeName}IntegrationBridge",
                ).joinToString("~"),
            )
            project.extensions.getByType(ExtraPropertiesExtension::class.java).set(
                "lazy.integration.metadataRecords",
                (extension.requiredMods.get() + extension.optionalMods.get()).joinToString("\n") { mod ->
                    "$integrationId~${extension.side.get().name}~$mod"
                },
            )
            project.extensions.configure<KspExtension> {
                arg("lazy.integration.id", integrationId)
                arg("lazy.integration.owner", extension.owner.get().name)
                arg("lazy.integration.side", extension.side.get().name)
                arg("lazy.integration.requiredMods", extension.requiredMods.get().joinToString(","))
                arg("lazy.integration.dependencies", extension.integrationDependencies.get().joinToString(","))
                arg("lazy.integration.dataGen", extension.dataGen.get().toString())
            }
        }

        project.gradle.projectsEvaluated {
            val dependencyProjects = linkedSetOf<Project>()
            val pending = ArrayDeque<Project>()
            listOf("api", "implementation").forEach { configurationName ->
                project.configurations.findByName(configurationName)?.dependencies
                    ?.withType(ProjectDependency::class.java)
                    ?.mapTo(pending) { dependency -> project.rootProject.project(dependency.path) }
            }
            while (pending.isNotEmpty()) {
                val dependencyProject = pending.removeFirst()
                if (dependencyProjects.add(dependencyProject)) {
                    listOf("api", "implementation").forEach { configurationName ->
                        dependencyProject.configurations.findByName(configurationName)?.dependencies
                            ?.withType(ProjectDependency::class.java)
                            ?.mapTo(pending) { dependency -> project.rootProject.project(dependency.path) }
                    }
                }
            }
            project.extensions.configure<NeoForgeExtension> {
                val testedMod = mods.getByName("lazy_test")
                dependencyProjects.forEach { dependencyProject ->
                    testedMod.sourceSet(
                        dependencyProject.extensions
                            .getByType(JavaPluginExtension::class.java)
                            .sourceSets
                            .getByName("main"),
                    )
                }
            }
        }
    }
}

public class LazyModPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("lazy.neoforge-library")
        project.pluginManager.apply("com.google.devtools.ksp")
        project.pluginManager.apply("maven-publish")
        project.dependencies.registerTransform(UnpackBundledModule::class.java) {
            from.attribute(ARTIFACT_FORMAT, "jar")
            to.attribute(ARTIFACT_FORMAT, BUNDLED_DIRECTORY_FORMAT)
        }
        project.tasks.register<RenderSvgTextures>("renderArtTextures") {
            group = "assets"
            description = "Render SVG art sources under art/ to the committed PNG texture paths."
            sourceDirectory.set(project.rootProject.layout.projectDirectory.dir("art"))
            textureDirectory.set(
                project.layout.projectDirectory.dir(
                    "src/main/resources/assets/${project.providers.gradleProperty("mod_id").get()}/textures",
                ),
            )
        }
    }

    private companion object {
        val ARTIFACT_FORMAT: Attribute<String> = Attribute.of("artifactType", String::class.java)
        const val BUNDLED_DIRECTORY_FORMAT: String = "lazy-bundled-directory"
    }
}

public class LazyDatagenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("lazy.neoforge-library")
        project.pluginManager.apply("com.google.devtools.ksp")
    }
}
