plugins {
    id("aap.conventions")
}

dependencies {
    implementation(libs.dbmigrering)
    implementation(libs.flywayDatabasePostgresql)
    runtimeOnly(libs.postgresql)
}
