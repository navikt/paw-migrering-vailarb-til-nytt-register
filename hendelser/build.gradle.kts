import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    schema("no.nav.paw.arbeidssokerregisteret.api.schema:eksternt-api:23.11.13.59-1")
    implementation("org.apache.avro:avro:1.11.1")
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    source(zipTree(schema.singleFile))
}
