plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("curios")
    requiredMod("curios", "curios_version_range")
    dataGen.set(true)
}

dependencies {
    compileOnly(libs.curios.api)
    add("integrationRuntime", libs.curios.runtime)
}
