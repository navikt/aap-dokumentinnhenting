val komponenterVersjon = "2.0.2"

plugins {
    id("aap.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:12.0.1")
    runtimeOnly("org.postgresql:postgresql:42.7.10")
}
