val komponenterVersjon = "1.0.375"

plugins {
    id("dokumentinnhenting.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:11.13.2")
    runtimeOnly("org.postgresql:postgresql:42.7.7")
}
