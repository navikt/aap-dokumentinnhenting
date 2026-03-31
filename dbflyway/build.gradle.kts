val komponenterVersjon = "2.0.24"

plugins {
    id("aap.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:12.2.0")
    runtimeOnly("org.postgresql:postgresql:42.7.10")
}
