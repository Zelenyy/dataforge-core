plugins {
    id("scientifik.mpp") version "0.1.4-dev" apply false
    id("scientifik.publish") version "0.1.4-dev" apply false
}

val dataforgeVersion by extra("0.1.3-dev-10")

val bintrayRepo by extra("dataforge")
val githubProject by extra("dataforge-core")

allprojects {
    group = "hep.dataforge"
    version = dataforgeVersion
}

subprojects {
    if (name.startsWith("dataforge")) {
        apply(plugin = "scientifik.publish")
    } 
}