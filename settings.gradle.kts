pluginManagement {
    repositories {
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://dl.bintray.com/korlibs/korlibs")
        maven("https://plugins.gradle.org/m2/")
        mavenCentral()
        google()
    }

    val korgePluginVersion: String by settings

    plugins {
        id("com.soywiz.korge") version korgePluginVersion
    }


    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.soywiz") {
                useModule("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:$korgePluginVersion")
            }
        }
    }
}

enableFeaturePreview("GRADLE_METADATA")