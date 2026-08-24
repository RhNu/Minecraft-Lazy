plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("appflux")
    requiredMod("appflux", "applied_flux_version_range")
    requiredMod("ae2", "ae2_version_range")
    requiredMod("glodium", "glodium_version_range")
    dependsOn("ae2")
}

dependencies {
    implementation(project(":integrations:ae2"))
    compileOnly(libs.ae2.api)
    compileOnly(libs.glodium)
    compileOnly(libs.applied.flux)
    add("integrationRuntime", libs.glodium)
    add("integrationRuntime", libs.applied.flux)
}
