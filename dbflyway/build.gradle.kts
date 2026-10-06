plugins {
    id("aap.conventions")
}

dependencies {
    implementation(libs.dbmigrering)
    runtimeOnly(kelvinLibs.postgresql)
}
