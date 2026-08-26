package rhx.lazy.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.mavenCentral

internal fun Project.configureLazyRepositories() {
    repositories.apply {
        mavenCentral()
        maven("https://maven.firstdark.dev/snapshots") { content { includeGroup("com.lowdragmc.ldlib2") } }
        maven("https://thedarkcolour.github.io/KotlinForForge/") { content { includeGroup("thedarkcolour") } }
        maven("https://maven.blamejared.com") { content { includeGroup("mezz.jei") } }
        maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
        maven("https://maven.latvian.dev/releases") {
            content {
                includeGroup("dev.latvian.mods")
                includeGroup("dev.latvian.apps")
            }
        }
        maven("https://jitpack.io") { content { includeGroup("com.github.rtyley") } }
        maven("https://maven.theillusivec4.top/") { content { includeGroup("top.theillusivec4.curios") } }
        maven("https://www.cursemaven.com") { content { includeGroup("curse.maven") } }
        maven("https://modmaven.dev") { content { includeGroup("mekanism") } }
        maven("https://maven.shadowsoffire.dev/releases") { content { includeGroup("dev.shadowsoffire") } }
    }
}
