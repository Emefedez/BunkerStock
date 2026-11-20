pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

val webOnly = providers.gradleProperty("webOnly").orNull?.toBoolean() == true
val androidOnly = providers.gradleProperty("androidOnly").orNull?.toBoolean() == true

rootProject.name = "BunkerStock"
include(":shared")
if (!webOnly) include(":app")
if (!androidOnly) include(":webApp")
