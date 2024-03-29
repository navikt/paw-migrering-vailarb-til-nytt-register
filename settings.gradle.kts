plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    kotlin("jvm") version "1.9.20" apply false
}

rootProject.name = "paw-migrering-vailarb-til-nytt-register"
include(
    "app",
    "veilarb-besvarelse",
    "veilarb-periode"
)

dependencyResolutionManagement {
    val githubPassword: String by settings
    repositories {
        maven {
            setUrl("https://maven.pkg.github.com/navikt/paw-observability")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        mavenCentral()
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
    }
    versionCatalogs {
        create("pawObservability") {
            from("no.nav.paw.observability:observability-version-catalog:24.02.20.10-1")
        }
    }
}
