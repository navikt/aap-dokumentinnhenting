import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    kotlin("jvm")
    alias(kelvinLibs.plugins.ktor)
    application
}

application {
    mainClass.set("dokumentinnhenting.AppKt")
}

dependencies {
    implementation(kelvinLibs.ktor.client.auth)
    implementation(kelvinLibs.ktor.client.jackson)
    implementation(kelvinLibs.ktor.client.logging)
    implementation(kelvinLibs.ktor.client.cio)
    implementation(kelvinLibs.ktor.client.content.negotiation)

    implementation(libs.brev.kontrakt)

    implementation(project(":dbflyway"))
    implementation(project(":kontrakt"))
    implementation(libs.ktor.openapi.generator)
    implementation(kelvinLibs.logstash.logback.encoder)
    implementation(kelvinLibs.nimbus.jose.jwt)
    implementation(kelvinLibs.hikaricp)
    implementation(kelvinLibs.caffeine)
    implementation(kelvinLibs.unleash.client.java)

    // Felleskomponenter
    implementation(libs.json)
    implementation(libs.infrastructure)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.dbtest)
    implementation(libs.motor)
    implementation(libs.motor.api)
    implementation(libs.server)
    implementation(libs.behandlingsflyt.kontrakt)

    // Tilgangsstyring
    implementation(libs.tilgang.plugin)

    // Kafka
    implementation(kelvinLibs.kafka.clients)
    implementation(kelvinLibs.kafka.streams)
    implementation(kelvinLibs.kafka.streams.test.utils)

    // Test
    testImplementation(kelvinLibs.ktor.server.test.host)
    testImplementation(kelvinLibs.testcontainers.postgresql)
    testImplementation(libs.motor.test.utils)
    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(kelvinLibs.mockk)
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
