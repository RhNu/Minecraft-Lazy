plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("appmek")
    requiredMod("appmek", "applied_mekanistics_version_range")
    requiredMod("ae2", "ae2_version_range")
    requiredMod("mekanism", "mekanism_version_range")
    dependsOn("ae2")
    dependsOn("mekanism")
}

dependencies {
    implementation(project(":integrations:ae2"))
    implementation(project(":integrations:mekanism"))
    compileOnly(libs.ae2.api)
    compileOnly(libs.mekanism)
    compileOnly(libs.applied.mekanistics)
    testImplementation(libs.ae2.api)
    testImplementation(libs.mekanism)
    testImplementation(libs.applied.mekanistics)
    add("integrationRuntime", libs.applied.mekanistics)
}
