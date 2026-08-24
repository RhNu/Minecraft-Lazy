import rhx.lazy.buildlogic.IntegrationOwner
import rhx.lazy.buildlogic.IntegrationSide

plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("jei")
    owner.set(IntegrationOwner.JEI)
    side.set(IntegrationSide.CLIENT)
    requiredMod("jei", "jei_version_range")
    dataGen.set(true)
}

dependencies {
    compileOnly(libs.jei)
    add("integrationRuntime", libs.jei)
}
