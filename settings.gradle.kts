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
    versionCatalogs {
        create("kelvinLibs") {
            from("no.nav.aap.kelvin:version-catalog:2.0.181")
        }
    }
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") {
            // Nav sine egne pakker (kelvin, behandlingsflyt, brev, tilgang, ...)
            content { includeGroupByRegex("no\\.nav\\..*") }
        }
        mavenCentral()
        mavenLocal()
    }
}
