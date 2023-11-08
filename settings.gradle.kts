pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "cli-qfj-dictionary-converter"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral {
            content {
                excludeGroup("com.exactpro.sf")
            }
        }

        // We ignore Gradle metadata because it contains strict limitations for Kotlin libraries
        // Which prevent dependency resolving
        // We are aware of possible problem with incompatible artifacts version
        // However we believe in Kotlin compatibility and hope that `1.6.0` and `1.9.0` versions a compatible
        maven {
            name = "Sonatype_snapshots"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            metadataSources {
                mavenPom()
                artifact()
                ignoreGradleMetadataRedirection()
            }
        }
        maven {
            name = "Sonatype_releases"
            url = uri("https://s01.oss.sonatype.org/content/repositories/releases/")
            metadataSources {
                mavenPom()
                artifact()
                ignoreGradleMetadataRedirection()
            }
        }

        mavenLocal()
    }
}