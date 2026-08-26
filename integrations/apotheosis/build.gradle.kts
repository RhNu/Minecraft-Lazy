plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("apotheosis")
    requiredMod("apotheosis", "apotheosis_version_range")
}

dependencies {
    compileOnly(libs.apotheosis)
    compileOnly(libs.apothic.attributes)
    compileOnly(libs.placebo)
    add("integrationRuntime", libs.apotheosis)
    add("integrationRuntime", libs.apothic.attributes)
    add("integrationRuntime", libs.placebo)
}
