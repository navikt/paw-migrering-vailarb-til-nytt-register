plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    kotlin("jvm") version "1.9.20" apply false
}


rootProject.name = "paw-migrering-vailarb-til-nytt-register"
include("app", "hendelser", "veilarb-besvarelse", "veilarb-periode")
