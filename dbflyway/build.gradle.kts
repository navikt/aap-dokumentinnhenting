val komponenterVersjon = "1.0.456"

plugins {
    id("dokumentinnhenting.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:11.18.0")
    runtimeOnly("org.postgresql:postgresql:42.7.8")
}
