plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("mekanism")
    requiredMod("mekanism", "mekanism_version_range")
    optionalMod("jei", "jei_version_range")
    dataGen.set(true)
}

dependencies {
    compileOnly(libs.mekanism)
    compileOnly(libs.jei)
    testImplementation(libs.mekanism)
    add("integrationRuntime", libs.mekanism)
}
