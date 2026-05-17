rootProject.name = "morphe-androidtv-patches"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()

        // Add this next if Morphe dependency fails to resolve
        // maven { url = uri("https://jitpack.io") }
    }
}
