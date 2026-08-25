plugins {
    id("lazy.neoforge-library")
}

description = "Compile-time lifecycle contracts shared by Lazy and its integration modules."

dependencies {
    testRuntimeOnly(libs.kotlinforforge)
}

kotlin {
    explicitApi()
}
