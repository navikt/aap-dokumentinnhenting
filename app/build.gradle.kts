import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.ByteArrayOutputStream

plugins {
    id("integrasjonportal.conventions")
    kotlin("jvm")
    id("io.ktor.plugin") version "3.0.0"
    application
}

val ktorVersion = "3.0.0"
val kafkaVersion = "3.8.0"
val komponenterVersjon = "1.0.25"
val confluentVersion = "7.7.1"

application {
    mainClass.set("integrasjonportal.AppKt")
}

dependencies {
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    implementation(project(":dbflyway"))
    implementation("no.nav:ktor-openapi-generator:1.0.46")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.5")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
    implementation("ch.qos.logback:logback-classic:1.5.9")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("com.nimbusds:nimbus-jose-jwt:9.41.2")
    implementation("org.flywaydb:flyway-database-postgresql:10.20.0")
    implementation("com.zaxxer:HikariCP:6.0.0")

    //Felleskomponenter
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")

    // kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("org.apache.kafka:kafka_2.13:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")

    // Test
    testImplementation(kotlin("test"))
    testImplementation("com.nimbusds:nimbus-jose-jwt:9.41.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:postgresql:1.20.2")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
}

fun runCommand(command: String): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = command.split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

fun getCheckedOutGitCommitHash(): String {
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        return System.getenv("GITHUB_SHA")
    }
    return runCommand("git rev-parse --verify HEAD")
}

tasks {
    val projectProps by registering(WriteProperties::class) {
        destinationFile = layout.buildDirectory.file("version.properties")
        // Define property.
        property("project.version", getCheckedOutGitCommitHash())
    }

    processResources {
        // Depend on output of the task to create properties,
        // so the properties file will be part of the Java resources.
        from(projectProps)
    }

    withType<ShadowJar> {
        mergeServiceFiles()
    }
    withType<Test> {
        useJUnitPlatform()
    }
}

kotlin.sourceSets["main"].kotlin.srcDirs("main/kotlin")
kotlin.sourceSets["test"].kotlin.srcDirs("test/kotlin")
sourceSets["main"].resources.srcDirs("main/resources")
sourceSets["test"].resources.srcDirs("test/resources")