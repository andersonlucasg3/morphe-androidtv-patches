group = "app.morphe"

patches {
    about {
        name = "andersonlucasg3 Patches"
        description = "Ad-blocking patches for Android TV streaming apps"
        source = "https://github.com/andersonlucasg3/morphe-androidtv-patches"
        author = "andersonlucasg3"
        contact = "https://github.com/andersonlucasg3"
        website = "https://github.com/andersonlucasg3/morphe-androidtv-patches"
        license = "GPLv3"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

dependencies {
    implementation(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"
        dependsOn(build)
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("ajstrick81.morphe.util.PatchListGeneratorKt")
    }
    publish {
        dependsOn("generatePatchesList")
    }
}
