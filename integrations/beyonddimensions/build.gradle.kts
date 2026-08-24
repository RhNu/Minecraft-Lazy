plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("beyonddimensions")
    requiredMod("beyonddimensions", "beyond_dimensions_version_range")
    dataGen.set(true)
}

dependencies {
    compileOnly(libs.beyond.dimensions)
    add("integrationRuntime", libs.beyond.dimensions)
}
