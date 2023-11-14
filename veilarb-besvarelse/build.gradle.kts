plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

dependencies {
    api("org.apache.avro:avro:1.11.0")
}
