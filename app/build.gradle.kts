import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
    id("io.ktor.plugin") version "2.3.8"
    id("com.google.cloud.tools.jib") version "3.4.0"
}

val jvmVersion = JavaVersion.VERSION_21
val image: String? by project

val agent by configurations.creating {
    isTransitive = false
}

val agentExtension by configurations.creating {
    isTransitive = false
}

val agentExtensionJar = "agent-extension.jar"
val agentJar = "agent.jar"
val agentFolder = layout.buildDirectory.dir("agent").get().toString()
val agentExtensionFolder = layout.buildDirectory.dir("agent-extension").get().toString()

val logbackVersion = "1.4.5"
val logstashVersion = "7.3"
val ktorVersion = pawObservability.versions.ktor
val navCommonModulesVersion = "2.2023.01.10_13.49-81ddc732df3a"
val hopliteVersion = "2.8.0.RC3"
val exposedVersion = "0.42.1"
val arbeidssokerregisteretVersion = "24.03.07.150-1"

dependencies {
    agent("io.opentelemetry.javaagent:opentelemetry-javaagent:${pawObservability.versions.openTelemetryInstrumentation.get()}")
    agentExtension("no.nav.paw.observability:opentelemetry-anonymisering-${pawObservability.versions.openTelemetryInstrumentation.get()}:24.02.20.10-1")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("org.flywaydb:flyway-core:9.21.2")
    implementation(project(":veilarb-besvarelse"))
    implementation(project(":veilarb-periode"))
    implementation("no.nav.paw.arbeidssokerregisteret.internt.schema:interne-eventer:$arbeidssokerregisteretVersion")
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-toml:$hopliteVersion")
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)

    implementation("no.nav.paw.arbeidssokerregisteret.api.schema:arbeidssoekerregisteret-kotlin:$arbeidssokerregisteretVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")

    // Ktor client
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion}")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("no.nav.common:token-client:$navCommonModulesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    implementation("org.apache.kafka:kafka-clients:3.5.1")
    implementation("org.apache.kafka:kafka-streams:3.5.1")
    implementation("io.confluent:kafka-avro-serializer:7.4.0")
    implementation("io.confluent:kafka-streams-avro-serde:7.4.0")
    implementation("org.apache.avro:avro:1.11.0")
    implementation("io.ktor:ktor-server-core-jvm:2.3.5")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:2.3.5")
    implementation("io.micrometer:micrometer-registry-prometheus:1.6.3")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.6.0")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.5.1")
    testImplementation(kotlin("reflect"))
}

application {
    mainClass.set("no.nav.paw.migrering.app.AppKt")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

tasks.create("addAgent", Copy::class) {
    from(agent)
    into(agentFolder)
    rename { _ -> agentJar}
}

tasks.create("addAgentExtension", Copy::class) {
    from(agentExtension)
    into(agentExtensionFolder)
    rename { _ -> agentExtensionJar}
}

tasks.withType(KotlinCompile::class) {
    dependsOn.add("addAgent")
    dependsOn.add("addAgentExtension")
}

jib {
    from.image = "ghcr.io/navikt/baseimages/temurin:${jvmVersion.majorVersion}"
    to.image = "${image ?: rootProject.name }:${project.version}"
    extraDirectories {
        paths {
            path {
                setFrom(agentFolder)
                into = "/app"
            }
            path {
                setFrom(agentExtensionFolder)
                into = "/app"
            }
        }
    }
    container.entrypoint = listOf(
        "java",
        "-cp", "@/app/jib-classpath-file",
        "-javaagent:/app/$agentJar",
        "-Dotel.javaagent.extensions=/app/$agentExtensionJar",
        "-Dotel.resource.attributes=service.name=${project.name}",
        application.mainClass.get()
    )
}
