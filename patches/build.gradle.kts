plugins {
    kotlin("jvm") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.morphe.patcher)
}

application {
    mainClass.set("patches.MainKt")
}
