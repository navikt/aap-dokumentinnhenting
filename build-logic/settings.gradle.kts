rootProject.name = "build-logic"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        mavenCentral()
    }
}
