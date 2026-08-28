import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    kotlin("jvm")
    alias(libs.plugins.ktor)
    application
}

application {
    mainClass.set("dokumentinnhenting.AppKt")
}

dependencies {
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.jackson)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    implementation(libs.brev.kontrakt)

    implementation(project(":dbflyway"))
    implementation(project(":kontrakt"))
    implementation(libs.ktor.openapi.generator)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.nimbus.jose.jwt)
    implementation(libs.hikari.cp)
    implementation(libs.caffeine)

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
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams)
    implementation(libs.kafka.streams.test.utils)

    // Test
    testImplementation(libs.bundles.test)
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
