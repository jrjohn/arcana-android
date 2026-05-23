// Nexus injection: when NEXUS_URL env is set (CI), prepend Nexus proxies
// as the first repos so they're tried before the public Google/MavenCentral.
// Without NEXUS_URL (local dev), the public repos are used directly.
// This replaces the previous init-script approach which couldn't bypass
// FAIL_ON_PROJECT_REPOS mode.
pluginManagement {
    repositories {
        val nexusUrl = System.getenv("NEXUS_URL")
        if (nexusUrl != null) {
            maven {
                url = uri("${nexusUrl}/gradle-plugins-proxy/")
                isAllowInsecureProtocol = true
            }
        }
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
        val nexusUrl = System.getenv("NEXUS_URL")
        if (nexusUrl != null) {
            maven {
                url = uri("${nexusUrl}/google-maven-proxy/")
                isAllowInsecureProtocol = true
            }
            maven {
                url = uri("${nexusUrl}/maven-central-proxy/")
                isAllowInsecureProtocol = true
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "aimodel"
include(":app")
