import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val javaVersion = JvmTarget.JVM_21
val silkVersion = "1.10.7"
val minecraftVersion = "1.21"

plugins {
    kotlin("jvm") version "2.0.0"
    id("fabric-loom") version "1.7-SNAPSHOT"
    kotlin("plugin.serialization") version "2.0.0"
    `maven-publish`
}

group = "gg.norisk"
version = "${minecraftVersion}-1.0.6"

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
    modImplementation("gg.norisk:emote-lib:${minecraftVersion}-1.0.9")

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

val sourceJar = tasks.register<Jar>("sourceJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

//TODO ja eig könnte man das multi gradle build skript machen aber wir wissen alle DAS M ULTIGRADLE HURENSÖHNE SIND
publishing {
    publications {
        create<MavenPublication>("binary") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
        }
        create<MavenPublication>("binaryAndSources") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
            artifact(sourceJar)
        }
    }
    repositories {
        fun MavenArtifactRepository.applyCredentials() = credentials {
            username = (System.getenv("NORISK_NEXUS_USERNAME") ?: project.findProperty("noriskMavenUsername")).toString()
            password = (System.getenv("NORISK_NEXUS_PASSWORD") ?: project.findProperty("noriskMavenPassword")).toString()
        }
        maven {
            name = "production"
            url = uri("https://maven.norisk.gg/repository/norisk-production/")
            applyCredentials()
        }
        maven {
            name = "dev"
            // this could also be a maven repo on the dev server
            // e.g. maven-staging.norisk.gg
            url = uri("https://maven.norisk.gg/repository/maven-releases/")
            applyCredentials()
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    val predicate = provider {
        (repository == publishing.repositories["production"] &&
                publication == publishing.publications["binary"]) ||
                (repository == publishing.repositories["dev"] &&
                        publication == publishing.publications["binaryAndSources"])
    }
    onlyIf("publishing binary to the production repository, or binary and sources to the internal dev one") {
        predicate.get()
    }
}

tasks.withType<PublishToMavenLocal>().configureEach {
    val predicate = provider {
        publication == publishing.publications["binaryAndSources"]
    }
    onlyIf("publishing binary and sources") {
        predicate.get()
    }
}

