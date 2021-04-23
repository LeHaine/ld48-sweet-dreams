plugins {
    id("com.soywiz.korge")
    kotlin("kapt") version "1.4.32"
}

korge {
    targetJvm()
    targetJs()
}

repositories {
    maven("https://jitpack.io")
}

val ldtkApiVersion: String by project
val kiwiVersion: String by project

kotlin {
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("$buildDir/generated/source/kaptKotlin/main")
            dependencies {
                implementation("com.lehaine.kt-ldtk-api:ldtk-api:$ldtkApiVersion")
                implementation("com.lehaine:kiwi:$kiwiVersion")
                //implementation("com.lehaine:ldtk-api:$ldtkApiVersion") // local repo
            }
        }

        val jvmMain by getting {
            dependencies {
                configurations.all { // kapt has an issue with determining the correct KMM library, so we need to help it
                    if (name.contains("kapt")) {
                        attributes.attribute(
                            org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute,
                            org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm // pass in the JVM
                        )
                    }
                }
                configurations["kapt"].dependencies.add(project.dependencies.create("com.lehaine.kt-ldtk-api:ldtk-processor:$ldtkApiVersion"))
                //configurations["kapt"].dependencies.add(project.dependencies.create("com.lehaine:ldtk-processor:$ldtkApiVersion")) // local repo
            }
        }
    }
}

tasks.getByName("compileKotlinMetadata").dependsOn("kaptKotlinJvm")

tasks {
    create("regenerateLDtkCode") {
        dependsOn("removeLDtkGeneratedCode")
        dependsOn("jvmProcessResources")
        dependsOn("generateLDtkCode")
    }

    create<Delete>("removeLDtkGeneratedCode") {
        delete = setOf("$buildDir/generated")
    }

    create("generateLDtkCode") {
        dependsOn("kaptKotlinJvm")
        mustRunAfter("removeLDtkGeneratedCode")
    }

}