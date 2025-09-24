pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    
    // Define plugin versions here to ensure consistency
    val kotlinVersion = "1.9.20"
    
    plugins {
        id("org.jetbrains.kotlin.android") version kotlinVersion
        id("org.jetbrains.kotlin.kapt") version kotlinVersion
    }
    
    resolutionStrategy {
        eachPlugin {
            when (requested.id.namespace) {
                "com.android" -> useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Lexify"
include(":app")
