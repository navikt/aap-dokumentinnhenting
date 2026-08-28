pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "dokumentinnhenting"

include(
    "app",
    "dbflyway",
    "kontrakt",
)

dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") {
            // Nav sine egne pakker (kelvin, behandlingsflyt, brev, tilgang, ...)
            content { includeGroupByRegex("no\\.nav\\..*") }
        }
        mavenCentral()
        maven("https://packages.confluent.io/maven/") {
            // Confluent Community Edition-bygg av Kafka, inkl. transitive io.confluent-avhengigheter
            content {
                includeGroup("org.apache.kafka")
                includeGroupByRegex("io\\.confluent.*")
            }
        }
        mavenLocal()
    }
}
