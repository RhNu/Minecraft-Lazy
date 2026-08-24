plugins {
    id("lazy.neoforge-library")
}

description = "Lazy runtime, core, and feature implementation."

dependencies {
    api(project(":integration-api"))
    api(libs.kotlinforforge)
    api(libs.ldlib2)
    compileOnly(libs.guideme.api)
}

tasks.test {
    systemProperty("lazy.projectDir", rootProject.projectDir.absolutePath)
}
