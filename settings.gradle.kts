pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.neoforged.net/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Lazy"

include(
    ":codegen:integration-annotations",
    ":codegen:integration-processor",
    ":integration-api",
    ":runtime",
    ":integrations:ae2",
    ":integrations:appflux",
    ":integrations:beyonddimensions",
    ":integrations:curios",
    ":integrations:jade",
    ":integrations:jei",
    ":integrations:kubejs",
    ":integrations:mekanism",
    ":integrations:mysticalagriculture",
    ":integrations:silentgear",
    ":mod",
    ":datagen",
)
