plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("mekanism")
    requiredMod("mekanism", "mekanism_version_range")
    dataGen.set(true)
}

dependencies {
    compileOnly(libs.mekanism)
    add("integrationRuntime", libs.mekanism)
}
