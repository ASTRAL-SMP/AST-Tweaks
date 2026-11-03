pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // 難読化あり（1.x）と難読化なし（26.1+）で Loom の変種を切り替える
    id("dev.kikugie.loom-back-compat") version "0.4.3"
}

stonecutter {
    create(rootProject) {
        versions("1.19.4", "1.20.1", "26.1.2", "26.2", "26.3")
        vcsVersion = "26.1.2"
    }
}

rootProject.name = "AST-Tweaks"
