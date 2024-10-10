plugins {
    id("aap-tilgang.conventions")
    kotlin("jvm")
    id("io.ktor.plugin") version "2.3.12"
    application
}

val ktorVersion = "2.3.12"
val aapLibVersion = "3.7.145"
val behandlingsflytVersjon = "0.0.19"
val postmottakVersjon = "0.0.10"

application {
    mainClass.set("tilgang.AppKt")
}

dependencies {
    implementation(project(":api-kontrakt"))
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")
    implementation("no.nav.aap.postmottak:kontrakt:$postmottakVersjon")
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

    implementation("dev.forst:ktor-openapi-generator:0.6.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.4")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("com.nimbusds:nimbus-jose-jwt:9.41.1")
    implementation("redis.clients:jedis:5.1.5")

    testImplementation(kotlin("test"))
    testImplementation("com.nimbusds:nimbus-jose-jwt:9.41.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.1")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.redis:testcontainers-redis:2.2.2")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}

kotlin.sourceSets["main"].kotlin.srcDirs("main/kotlin")
kotlin.sourceSets["test"].kotlin.srcDirs("test/kotlin")
sourceSets["main"].resources.srcDirs("main/resources")
sourceSets["test"].resources.srcDirs("test/resources")