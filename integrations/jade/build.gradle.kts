import rhx.lazy.buildlogic.IntegrationOwner

plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("jade")
    owner.set(IntegrationOwner.JADE)
    requiredMod("jade", "jade_version_range")
    optionalMod("mysticalagriculture", "mystical_agriculture_version_range")
    dataGen.set(true)
}

dependencies {
    implementation(project(":integrations:mysticalagriculture"))
    compileOnly(libs.jade)
    add("integrationRuntime", libs.jade)
}
