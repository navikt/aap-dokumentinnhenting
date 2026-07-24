plugins {
    id("aap.conventions")
}

dependencies {
    implementation(libs.dbmigrering)
    runtimeOnly(libs.postgresql)
}
