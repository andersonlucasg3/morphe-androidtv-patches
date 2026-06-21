extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "ajstrick81.morphe.extension"
}

dependencies {
    // okhttp3 is already bundled in Peacock's app at runtime — only needed
    // here to compile PeacockAdPatchHelper's invoke-static call target.
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")

    // Guava is already bundled in Prime Video's app at runtime — only needed
    // here to compile SkipAdsPatch's ImmutableMap parameter/return types.
    compileOnly("com.google.guava:guava:33.5.0-jre")
}
