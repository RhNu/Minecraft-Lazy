plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("mysticalagriculture")
    requiredMod("mysticalagriculture", "mystical_agriculture_version_range")
    optionalMod("mysticalagradditions", "mystical_agradditions_version_range")
    dataGen.set(true)
}

dependencies {
    compileOnly(libs.mystical.agriculture)
    compileOnly(libs.mystical.agradditions)
    compileOnly(libs.cucumber)
    add("integrationRuntime", libs.cucumber)
    add("integrationRuntime", libs.mystical.agriculture)
    add("integrationRuntime", libs.mystical.agradditions)
}
