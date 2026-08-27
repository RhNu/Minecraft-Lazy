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
    ":integrations:appmek",
    ":integrations:apotheosis",
    ":integrations:appflux",
    ":integrations:bhc",
    ":integrations:beyonddimensions",
    ":integrations:curios",
    ":integrations:avaritia-delight",
    ":integrations:corn-delight",
    ":integrations:crabbersdelight",
    ":integrations:delighto-flight",
    ":integrations:eternal-starlight-delight",
    ":integrations:farmersdelight",
    ":integrations:fruitsdelight",
    ":integrations:jade",
    ":integrations:jei",
    ":integrations:kubejs",
    ":integrations:mooncake-delight",
    ":integrations:pineapple-delight",
    ":integrations:rusticdelight",
    ":integrations:kaleidoscope-cookery",
    ":integrations:kaleidoscope-end",
    ":integrations:kaleidoscope-grilling",
    ":integrations:kaleidoscope-nether",
    ":integrations:kaleidoscope-tavern",
    ":integrations:mekanism",
    ":integrations:mysticalagriculture",
    ":integrations:silentgear",
    ":integrations:tacz",
    ":integrations:twilightdelight",
    ":integrations:ubesdelight",
    ":integrations:veggiesdelight",
    ":integrations:vintagedelight",
    ":mod",
    ":datagen",
)
