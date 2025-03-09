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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Alfred 2017 Redux"
include(":app")

include(":smartfoo-android-lib-core")
project(":smartfoo-android-lib-core").projectDir = file("../../SmartFoo/smartfoo/android/smartfoo-android-lib-core/")
