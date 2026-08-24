plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("ae2")
    requiredMod("ae2", "ae2_version_range")
    dataGen.set(true)
}

dependencies {
    compileOnly(libs.ae2.api)
    add("integrationRuntime", libs.ae2.api)
}
