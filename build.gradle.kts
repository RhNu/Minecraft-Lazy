plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
}

group = providers.gradleProperty("mod_group_id").get()
version = providers.gradleProperty("mod_version").get()

val aggregateProjects = subprojects.filter { subproject -> subproject.childProjects.isEmpty() }

tasks.named("check") {
    dependsOn(aggregateProjects.map { subproject -> "${subproject.path}:check" })
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}

tasks.named("build") {
    dependsOn(":mod:build")
}

listOf(
    "runClient",
    "runServer",
    "runClientIntegrations",
    "runServerIntegrations",
    "runGameTestServer",
    "renderArtTextures",
).forEach { taskName ->
    tasks.register(taskName) {
        group = if (taskName.startsWith("run")) "mod development" else "assets"
        dependsOn(":mod:$taskName")
    }
}

tasks.register("runData") {
    group = "mod development"
    dependsOn(":datagen:runData")
}
