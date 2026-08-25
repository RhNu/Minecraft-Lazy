import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.attributes.Attribute
import org.gradle.jvm.tasks.Jar
import org.slf4j.event.Level
import rhx.lazy.buildlogic.ValidateIntegrationManifest

plugins {
    id("lazy.mod")
    idea
}

val modId = providers.gradleProperty("mod_id").get()
val integrationProjects = project(":integrations").subprojects.sortedBy { integrationProject -> integrationProject.path }
val bundledProjects = listOf(project(":integration-api"), project(":runtime")) + integrationProjects

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("mod_group_id").get()

base { archivesName.set(modId) }

sourceSets.main {
    resources {
        srcDir("src/generated/resources")
        exclude("**/*.bbmodel", "**/.gitkeep", "**/.cache/**")
    }
}

val localRuntime by configurations.creating
val clientIntegrationsRuntime by configurations.creating
val serverIntegrationsRuntime by configurations.creating
val bundledModules by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}
val bundledModuleDirectories =
    bundledModules.incoming.artifactView {
        attributes.attribute(Attribute.of("artifactType", String::class.java), "lazy-bundled-directory")
    }.files

val clientIntegrationsRun by sourceSets.creating { runtimeClasspath += sourceSets.main.get().output }
val serverIntegrationsRun by sourceSets.creating { runtimeClasspath += sourceSets.main.get().output }

configurations.named(clientIntegrationsRun.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}
configurations.named(clientIntegrationsRun.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.runtimeOnly.get(), localRuntime, clientIntegrationsRuntime)
}
configurations.named(serverIntegrationsRun.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}
configurations.named(serverIntegrationsRun.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.runtimeOnly.get(), localRuntime, serverIntegrationsRuntime)
}
configurations.runtimeClasspath { extendsFrom(localRuntime) }

bundledProjects.forEach { evaluationDependsOn(it.path) }

val integrationCatalogSpec =
    integrationProjects.joinToString("\n") { integrationProject ->
        integrationProject.extensions.extraProperties.get("lazy.integration.catalogRecord").toString()
    }

data class IntegrationCatalogRecord(
    val id: String,
    val owner: String,
    val side: String,
    val requiredMods: List<String>,
    val dependencies: List<String>,
    val dataGen: Boolean,
    val bridgeClass: String,
)

val integrationCatalogRecords =
    integrationCatalogSpec.lineSequence().filter(String::isNotBlank).associate { record ->
        val fields = record.split('~')
        require(fields.size == 7) { "Malformed integration catalog record: $record" }
        val parsed =
            IntegrationCatalogRecord(
                id = fields[0],
                owner = fields[1],
                side = fields[2],
                requiredMods = fields[3].split(',').filter(String::isNotBlank),
                dependencies = fields[4].split(',').filter(String::isNotBlank),
                dataGen = fields[5].toBooleanStrict(),
                bridgeClass = fields[6],
            )
        parsed.id to parsed
    }
val integrationProjectsById =
    integrationProjects.associateBy { integrationProject ->
        integrationProject.extensions.extraProperties
            .get("lazy.integration.catalogRecord")
            .toString()
            .substringBefore('~')
    }

data class IntegrationMixinRecord(
    val integrationId: String,
    val config: String,
    val requiredModIds: List<String>,
)

val integrationMixinRecords =
    integrationProjects.flatMap { integrationProject ->
        val integrationId =
            integrationProject.extensions.extraProperties
                .get("lazy.integration.catalogRecord")
                .toString()
                .substringBefore('~')
        val requiredModIds =
            integrationCatalogRecords
                .getValue(integrationId)
                .requiredMods
                .map { requirement -> requirement.substringBefore('|') }
                .distinct()
                .sorted()
        integrationProject.extensions.extraProperties
            .get("lazy.integration.mixinConfigs")
            .toString()
            .split(',')
            .filter(String::isNotBlank)
            .map { config -> IntegrationMixinRecord(integrationId, config, requiredModIds) }
    }

val duplicateMixinConfigs = integrationMixinRecords.groupBy(IntegrationMixinRecord::config).filterValues { it.size > 1 }
require(duplicateMixinConfigs.isEmpty()) {
    "Mixin configs must be owned by exactly one integration: " +
        duplicateMixinConfigs.mapValues { (_, records) -> records.map(IntegrationMixinRecord::integrationId) }
}

val integrationMetadataRecords =
    integrationProjects
        .flatMap { integrationProject ->
            integrationProject.extensions.extraProperties
                .get("lazy.integration.metadataRecords")
                .toString()
                .lineSequence()
                .filter(String::isNotBlank)
                .toList()
        }.map { record ->
            val fields = record.split('~')
            require(fields.size == 3) { "Malformed integration metadata record: $record" }
            val integrationId = fields[0]
            val integrationSide = fields[1]
            val modFields = fields[2].split('|')
            require(modFields.size == 2) { "Malformed integration mod constraint: ${fields[2]}" }
            IntegrationMetadataRecord(integrationId, integrationSide, modFields[0], modFields[1])
        }

data class IntegrationMetadataRecord(
    val integrationId: String,
    val side: String,
    val modId: String,
    val versionProperty: String,
)

extensions.configure<KspExtension> {
    arg("lazy.integration.catalog", integrationCatalogSpec)
}

neoForge {
    addModdingDependenciesTo(clientIntegrationsRun)
    addModdingDependenciesTo(serverIntegrationsRun)

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("clientIntegrations") {
            client()
            sourceSet = clientIntegrationsRun
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("serverIntegrations") {
            server()
            sourceSet = serverIntegrationsRun
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = Level.DEBUG
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            bundledProjects.forEach { bundledProject -> sourceSet(bundledProject.sourceSets.main.get()) }
        }
    }

    unitTest {
        testedMod = mods[modId]
    }
}

val knownIntegrations = integrationCatalogRecords.keys
val requestedIntegrationProperty = providers.gradleProperty("lazy.integrations").orNull
val requestedIntegrations =
    requestedIntegrationProperty?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.toSet() ?: knownIntegrations
val unknownIntegrations = requestedIntegrations - knownIntegrations
require(unknownIntegrations.isEmpty()) {
    "Unknown Lazy integrations: ${unknownIntegrations.sorted()}; expected one of ${knownIntegrations.sorted()}"
}
val selectedIntegrations = buildSet<String> {
    val pending = ArrayDeque(requestedIntegrations.sorted())
    while (pending.isNotEmpty()) {
        val integrationId = pending.removeFirst()
        if (add(integrationId)) {
            integrationCatalogRecords.getValue(integrationId).dependencies.sorted().forEach(pending::addLast)
        }
    }
}
val runsServerIntegrations = gradle.startParameter.taskNames.any { taskName -> taskName.endsWith("runServerIntegrations") }
if (requestedIntegrationProperty != null && runsServerIntegrations) {
    val clientOnlySelections = selectedIntegrations.filter { integrationCatalogRecords.getValue(it).side == "CLIENT" }
    require(clientOnlySelections.isEmpty()) {
        "Client-only Lazy integrations cannot be selected for runServerIntegrations: ${clientOnlySelections.sorted()}"
    }
}

dependencies {
    compileOnly(project(":codegen:integration-annotations"))
    ksp(project(":codegen:integration-processor"))
    bundledProjects.forEach { bundledProject ->
        implementation(bundledProject)
        bundledModules(bundledProject)
    }
    localRuntime(libs.guideme.api)
    testRuntimeOnly(libs.guideme.api)
    selectedIntegrations.sorted().forEach { integrationId ->
        val integrationProject = integrationProjectsById.getValue(integrationId)
        val runtimeDependency =
            project(
                mapOf(
                    "path" to integrationProject.path,
                    "configuration" to "integrationRuntimeElements",
                ),
            )
        clientIntegrationsRuntime(runtimeDependency)
        if (integrationCatalogRecords.getValue(integrationId).side != "CLIENT") {
            serverIntegrationsRuntime(runtimeDependency)
        }
    }
}

val generateModMetadata by tasks.registering(ProcessResources::class) {
    val replacements =
        providers.provider {
            val baseProperties =
                listOf(
                    "minecraft_version_range",
                    "neo_version",
                    "loader_version_range",
                    "mod_id",
                    "mod_name",
                    "mod_license",
                    "mod_version",
                    "mod_authors",
                    "mod_description",
                    "ldlib2_version_range",
                    "guideme_version_range",
                ).associateWith { key -> providers.gradleProperty(key).get() }
            val integrationMixins =
                integrationMixinRecords.joinToString("\n\n") { record ->
                    val requiredMods = record.requiredModIds.joinToString(", ") { modId -> "\"$modId\"" }
                    """
                    [[mixins]]
                    config="${record.config}"
                    requiredMods=[$requiredMods]
                    """.trimIndent()
                }
            val integrationDependencies =
                integrationMetadataRecords
                    .groupBy(IntegrationMetadataRecord::modId)
                    .toSortedMap()
                    .map { (dependencyModId, declarations) ->
                        val versionProperties = declarations.map(IntegrationMetadataRecord::versionProperty).distinct()
                        require(versionProperties.size == 1) {
                            "Conflicting version properties for '$dependencyModId': $versionProperties"
                        }
                        val sides = declarations.map(IntegrationMetadataRecord::side).distinct()
                        val side = if (sides == listOf("CLIENT")) "CLIENT" else "BOTH"
                        val owners = declarations.map(IntegrationMetadataRecord::integrationId).distinct().sorted()
                        """
                        [[dependencies.$modId]]
                        modId="$dependencyModId"
                        type="optional"
                        versionRange="${providers.gradleProperty(versionProperties.single()).get()}"
                        ordering="AFTER"
                        side="$side"
                        reason="Enables Lazy integration(s): ${owners.joinToString(", ")}"
                        """.trimIndent()
                    }.joinToString("\n\n")
            baseProperties +
                mapOf(
                    "integration_mixins" to integrationMixins,
                    "integration_dependencies" to integrationDependencies,
                )
        }
    inputs.properties(replacements.get())
    doFirst { expand(replacements.get()) }
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.main { resources.srcDir(generateModMetadata) }
neoForge.ideSyncTask(generateModMetadata)

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.FAIL
    includeEmptyDirs = false
    from(bundledModuleDirectories)
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.FAIL
    includeEmptyDirs = false
    bundledProjects.forEach { bundledProject ->
        from(bundledProject.extensions.getByType<JavaPluginExtension>().sourceSets.getByName("main").allSource)
    }
}

val validateIntegrationManifest by tasks.registering(ValidateIntegrationManifest::class) {
    group = "verification"
    description = "Verify the generated KSP integration manifest matches the Gradle DSL descriptors."
    dependsOn("kspKotlin")
    manifestFile.set(layout.buildDirectory.file("generated/ksp/main/resources/META-INF/lazy/integrations.json"))
    expectedRecords.set(integrationCatalogSpec.lineSequence().filter(String::isNotBlank).toList())
}

tasks.named("check") {
    dependsOn(validateIntegrationManifest)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = modId
            artifact(tasks.named("jar"))
            artifact(tasks.named("sourcesJar"))
        }
    }
    repositories {
        maven {
            name = "projectLocal"
            url = uri(rootProject.layout.projectDirectory.dir("repo"))
        }
    }
}

idea.module {
    isDownloadSources = true
    isDownloadJavadoc = true
}
