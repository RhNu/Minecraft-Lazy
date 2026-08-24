plugins {
    `kotlin-dsl`
}

group = "rhx.lazy.buildlogic"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.moddev.gradle.plugin)
    implementation(libs.batik.transcoder)
    implementation(libs.batik.codec)
}

gradlePlugin {
    plugins {
        register("lazyKotlinLibrary") {
            id = "lazy.kotlin-library"
            implementationClass = "rhx.lazy.buildlogic.LazyKotlinLibraryPlugin"
        }
        register("lazyNeoForgeLibrary") {
            id = "lazy.neoforge-library"
            implementationClass = "rhx.lazy.buildlogic.LazyNeoForgeLibraryPlugin"
        }
        register("lazyIntegration") {
            id = "lazy.integration"
            implementationClass = "rhx.lazy.buildlogic.LazyIntegrationPlugin"
        }
        register("lazyMod") {
            id = "lazy.mod"
            implementationClass = "rhx.lazy.buildlogic.LazyModPlugin"
        }
        register("lazyDatagen") {
            id = "lazy.datagen"
            implementationClass = "rhx.lazy.buildlogic.LazyDatagenPlugin"
        }
    }
}
