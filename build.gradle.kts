import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.slf4j.event.Level

plugins {
    `java-library`
    `maven-publish`
    idea
    id("net.neoforged.moddev") version "2.0.142"
    kotlin("jvm") version "2.4.0"
}

val modId = property("mod_id").toString()

version = property("mod_version").toString()
group = property("mod_group_id").toString()

base {
    archivesName.set(modId)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}

sourceSets.main {
    resources {
        srcDir("src/generated/resources")
        exclude("**/*.bbmodel")
        exclude("**/.gitkeep")
        exclude("src/generated/**/.cache")
    }
}

neoForge {
    version = property("neo_version").toString()

    parchment {
        minecraftVersion = property("parchment_minecraft_version").toString()
        mappingsVersion = property("parchment_mappings_version").toString()
    }

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

        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("data") {
            data()
            programArguments.addAll(
                "--mod",
                modId,
                "--all",
                "--output",
                file("src/generated/resources").absolutePath,
                "--existing",
                file("src/main/resources").absolutePath,
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = Level.DEBUG
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val localRuntime by configurations.creating
configurations.runtimeClasspath {
    extendsFrom(localRuntime)
}

repositories {
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        content {
            includeGroup("thedarkcolour")
        }
    }
    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
        content {
            includeGroup("mezz.jei")
        }
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:${property("kotlin_for_forge_version")}")

    localRuntime("mezz.jei:jei-${property("minecraft_version")}-neoforge:${property("jei_version")}")
    localRuntime("maven.modrinth:jade:${property("jade_version")}")
}

val generateModMetadata by tasks.registering(ProcessResources::class) {
    val replacements =
        mapOf(
            "minecraft_version_range" to project.property("minecraft_version_range"),
            "neo_version" to project.property("neo_version"),
            "loader_version_range" to project.property("loader_version_range"),
            "mod_id" to project.property("mod_id"),
            "mod_name" to project.property("mod_name"),
            "mod_license" to project.property("mod_license"),
            "mod_version" to project.property("mod_version"),
            "mod_authors" to project.property("mod_authors"),
            "mod_description" to project.property("mod_description"),
        )

    inputs.properties(replacements)
    expand(replacements)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.main {
    resources.srcDir(generateModMetadata)
}
neoForge.ideSyncTask(generateModMetadata)

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = modId
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "projectLocal"
            url = uri("repo")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
