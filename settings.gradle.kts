@file:Suppress("UnstableApiUsage")

import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.2.0"
}

dependencyResolutionManagement {
    repositories {
        repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

        intellijPlatform { defaultRepositories() } //for ide plugin
        maven("https://packages.jetbrains.team/maven/p/kpm/public") //for jewel compose theme

        google {
            mavenContent {
                includeGroupByRegex(".*android.*")
                includeGroupByRegex(".*androidx.*")
                includeGroupByRegex(".*google.*")
            }
        }

        //firework libraries
        maven("https://packages.jetbrains.team/maven/p/firework/dev") {
            mavenContent {
                includeGroupByRegex("org.jetbrains.compose.*")
            }
        }

        mavenCentral()
    }
}

include(":idea-plugin")
