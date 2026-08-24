import rhx.lazy.buildlogic.IntegrationOwner

plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("kubejs")
    owner.set(IntegrationOwner.KUBEJS)
    requiredMod("kubejs", "kubejs_version_range")
}

dependencies {
    compileOnly(libs.kubejs)
    add("integrationRuntime", libs.kubejs)
}
