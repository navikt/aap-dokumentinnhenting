import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("aap.conventions")
    `maven-publish`
    `java-library`
}

group = "no.nav.aap.dokumentinnhenting"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
            version = project.findProperty("version")?.toString() ?: "0.0.0"
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/aap-dokumentinnhenting")
            credentials {
                username = "x-access-token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
}
