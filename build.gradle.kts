import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val javaVersion = JvmTarget.JVM_21
val silkVersion = "1.10.7"
val minecraftVersion = "1.21"

plugins {
    kotlin("jvm") version "2.0.0"
    id("fabric-loom") version "1.7-SNAPSHOT"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "org.example"
version = "1.0.0"

repositories {
    mavenCentral()

    maven {
        url = uri("https://maven.norisk.gg/repository/maven-releases/")
        credentials {
            username = (System.getenv("NORISK_NEXUS_USERNAME") ?: project.findProperty("noriskMavenUsername") ?: "").toString()
            password = (System.getenv("NORISK_NEXUS_PASSWORD") ?: project.findProperty("noriskMavenPassword") ?: "").toString()
        }
    }
    maven {
        url = uri("https://maven.norisk.gg/repository/norisk-production/")
        credentials {
            username = (System.getenv("NORISK_NEXUS_USERNAME") ?: project.findProperty("noriskMavenUsername") ?: "").toString()
            password = (System.getenv("NORISK_NEXUS_PASSWORD") ?: project.findProperty("noriskMavenPassword") ?: "").toString()
        }
    }
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    maven { url = uri("https://maven.wispforest.io") }

    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21")
    mappings("net.fabricmc:yarn:1.21+build.9")
    modImplementation("net.fabricmc:fabric-loader:0.16.0")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.100.7+1.21")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.10.19+kotlin.1.9.23")

    modImplementation("gg.norisk:datatracker:${minecraftVersion}-1.0.7")
    modImplementation("gg.norisk:emote-lib:${minecraftVersion}-1.0.8")

    val geckolibVersion = "1.21:4.5.6"

    modImplementation("io.wispforest:owo-lib:0.12.10+${minecraftVersion}")
    modImplementation("software.bernie.geckolib:geckolib-fabric-$geckolibVersion")

    modImplementation("maven.modrinth:sodium:mc1.21-0.5.11")
    modImplementation("maven.modrinth:nvidium:0.2.9-beta")
    modImplementation("maven.modrinth:auth-me:8.0.0+1.21")
    modImplementation("maven.modrinth:cloth-config:15.0.130+fabric")

    modImplementation("gg.norisk:noriskclient-fabric-utils:${minecraftVersion}-2.1.16")
    modImplementation("gg.norisk:noriskclient-zoom:${minecraftVersion}-2.0.2")
    modImplementation("gg.norisk:ui:${minecraftVersion}-2.2.15")
    modImplementation("gg.norisk:fullbright:${minecraftVersion}-2.0.0")
    modImplementation("gg.norisk:nametags:${minecraftVersion}-2.0.0")
    modImplementation("gg.norisk:nametags:${minecraftVersion}-2.0.0")
    modImplementation("gg.norisk:freelook:${minecraftVersion}-2.0.2")

    modImplementation("net.silkmc:silk-core:$silkVersion")
    modImplementation("net.silkmc:silk-network:$silkVersion")
    modImplementation("net.silkmc:silk-commands:$silkVersion")
}

tasks {
    compileKotlin {
        compilerOptions {
            freeCompilerArgs = listOf("-Xjdk-release=${javaVersion.target}", "-Xskip-prerelease-check")
            jvmTarget.set(javaVersion)
        }
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(javaVersion.target.toInt())
    }
    processResources {
        val properties = mapOf("version" to project.version)
        inputs.properties(properties)
        filesMatching("fabric.mod.json") { expand(properties) }
    }
}
