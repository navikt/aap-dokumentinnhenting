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
    implementation(libs.ktorServerAuth)
    implementation(libs.ktorServerAuthJwt)
    implementation(libs.ktorServerCallLogging)
    implementation(libs.ktorServerCallLoggingJvm)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerMetricsMicrometer)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerStatusPages)

    implementation(libs.ktorClientAuth)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorClientJackson)
    implementation(libs.ktorClientLogging)

    implementation(libs.brevKontrakt)

    implementation(project(":dbflyway"))
    implementation(project(":kontrakt"))
    implementation(libs.ktorOpenApiGenerator)
    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.ktorSerializationJackson)
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.jacksonModuleKotlin)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.nimbusJoseJwt)
    implementation(libs.flywayDatabasePostgresql)
    implementation(libs.hikariCp)
    implementation(libs.caffeine)

    // Felleskomponenter
    implementation(libs.json)
    implementation(libs.infrastructure)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.dbtest)
    implementation(libs.motor)
    implementation(libs.motorApi)
    implementation(libs.server)
    implementation(libs.behandlingsflytKontrakt)

    // Tilgangsstyring
    implementation(libs.tilgangPlugin)

    // Kafka
    implementation(libs.kafkaClients)
    implementation(libs.kafkaStreams)
    implementation(libs.kafkaStreamsTestUtils)

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
