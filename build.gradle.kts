import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files

plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("io.papermc.paperweight.userdev") version "1.7.2"
}

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://eldonexus.de/repository/maven-releases/")
}

group = "club.devcord.gamejam"
version = "1.0.0"
description = "gamejam"

dependencies {
    testImplementation("junit:junit:4.13.2")
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    compileOnly("de.chojo.pluginjam:plugin-paper:1.0.3")
}

paperweight {
    reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    runServer {
        minecraftVersion("1.21.1")
    }

    register("uploadJar") {
        dependsOn(jar)

        doLast {
            val filePath = project.tasks.jar.get().archiveFile.get().asFile.toPath()
            // Add the upload api url to your repo secrets under UPLOAD_URL
            // Obtain the upload url via /team api
            val apiUrl = System.getenv("UPLOAD_URL") ?: throw RuntimeException("Please set UPLOAD_URL in your github secrets")
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                //.uri(URI.create("$apiUrl?restart=true"))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(Files.readAllBytes(filePath)))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 202) throw RuntimeException("Could not upload:\n" + response.body())
            println("Upload successful")
        }
    }
}

bukkit {
    name = "GameJamPlugin"
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
    main = "club.devcord.gamejam.JamPlugin"
    apiVersion = "1.21"
}
