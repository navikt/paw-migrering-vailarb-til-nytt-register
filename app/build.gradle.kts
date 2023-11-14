plugins {
//    id("org.gradle.toolchains.foojay-resolver-convention")
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {

}

application {
    mainClass.set("no.nav.paw.migrering.app.AppKt")
}
