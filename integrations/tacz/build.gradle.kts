plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("tacz")
    requiredMod("tacz", "tacz_version_range")
    mixinConfig("lazy.tacz.mixins.json")
    dataGen.set(true)
}

dependencies {
    compileOnly(libs.tacz)
    add("integrationRuntime", libs.tacz)
}
