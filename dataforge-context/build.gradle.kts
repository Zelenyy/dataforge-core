plugins {
    kotlin("multiplatform")
}

description = "Context and provider definitions"

val coroutinesVersion: String by rootProject.extra

kotlin {
    jvm()
    js()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-meta"))
                api(kotlin("reflect"))
                api("io.github.microutils:kotlin-logging-common:1.6.10")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutinesVersion")
            }
        }
        val jvmMain by getting {
            dependencies {
                api("io.github.microutils:kotlin-logging:1.6.10")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }
        val jsMain by getting {
            dependencies {
                api("io.github.microutils:kotlin-logging-js:1.6.10")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
            }
        }
    }
}