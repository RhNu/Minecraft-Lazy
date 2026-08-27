plugins {
    id("lazy.integration")
}

lazyIntegration {
    id.set("bhc")
    requiredMod("bhc", "baubley_heart_canisters_version_range")
}

dependencies {
    compileOnly(libs.baubley.heart.canisters)
    add("integrationRuntime", libs.baubley.heart.canisters)
}
