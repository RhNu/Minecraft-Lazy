plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("silentgear")
    requiredMod("silentgear", "silent_gear_version_range")
}

dependencies {
    compileOnly(libs.silent.gear)
    compileOnly(libs.silent.lib)
    add("integrationRuntime", libs.silent.gear)
    add("integrationRuntime", libs.silent.lib)
}
