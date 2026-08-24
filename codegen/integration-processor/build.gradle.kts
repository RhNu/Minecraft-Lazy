plugins {
    id("lazy.kotlin-library")
}

description = "KSP processor that validates and generates Lazy integration bridges."

kotlin {
    explicitApi()
}

dependencies {
    implementation(project(":codegen:integration-annotations"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.ksp.api)
}
