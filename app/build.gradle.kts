import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    kotlin("jvm")
    id("io.ktor.plugin") version "3.5.1"
    application
}

val ktorVersion = "3.5.1"
val kafkaVersion = "8.1.1-ce"
val komponenterVersjon = "2.0.81"
val behandlingsflytVersjon = "0.0.627"
val tilgangVersjon = "1.0.227"
val jacksonVersjon = "2.22.0"

application {
    mainClass.set("dokumentinnhenting.AppKt")
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
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    implementation("no.nav.aap.brev:kontrakt:0.0.277")

    implementation(project(":dbflyway"))
    implementation(project(":kontrakt"))
    implementation("no.nav.aap.kelvin:ktor-openapi-generator:$komponenterVersjon")
    implementation("io.micrometer:micrometer-registry-prometheus:1.17.0")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersjon")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersjon") // Use the latest version
    implementation("ch.qos.logback:logback-classic:1.5.37")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("com.nimbusds:nimbus-jose-jwt:10.9.1")
    implementation("org.flywaydb:flyway-database-postgresql:12.9.0")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")

    // Felleskomponenter
    implementation("no.nav.aap.kelvin:json:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")

    // Tilgangsstyring
    implementation("no.nav.aap.tilgang:plugin:$tilgangVersjon")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")

    // Test
    testImplementation(kotlin("test"))
    testImplementation("com.nimbusds:nimbus-jose-jwt:10.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.1.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
    testImplementation("no.nav.aap.kelvin:motor-test-utils:$komponenterVersjon")
    testImplementation("io.mockk:mockk:1.14.11")
}

tasks {
    withType<ShadowJar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }
}

kotlin.sourceSets["main"].kotlin.srcDirs("main/kotlin")
kotlin.sourceSets["test"].kotlin.srcDirs("test/kotlin")
sourceSets["main"].resources.srcDirs("main/resources")
sourceSets["test"].resources.srcDirs("test/resources")
