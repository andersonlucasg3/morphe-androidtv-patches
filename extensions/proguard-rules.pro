# ProGuard rules for Prime Video ATV extensions.
#
# The extension classes are injected into the patched app's DEX at build time.
# These rules prevent R8/ProGuard from stripping or renaming the static entry
# points that are called directly from patched smali bytecode via invoke-static.
# If these methods are renamed, the invoke-static descriptors in the patch will
# not resolve at runtime and the patch will silently have no effect.

-keep class ajstrick81.morphe.extension.primevideo.ads.SkipAdsPatch {
    public static *** skipAllMedia3AdGroups(com.google.common.collect.ImmutableMap);
    public static *** skipAllExo2AdGroups(com.google.common.collect.ImmutableMap);
}

-keep class ajstrick81.morphe.extension.tubi.ads.SkipAdsPatch {
    public static *** onClearVodAds(java.lang.Object);
    public static *** shouldBlock(java.lang.Object);
}

-keep class ajstrick81.morphe.extension.peacock.ads.SkipAdsPatch {
    public static *** emptyAdPlaybackState(java.lang.Object);
}
# Peacock — Layer 6: OkHttp ad CDN interceptor
# Morphe loads this via reflection when injecting into NetworkingKt.getOkHttpClient().
# Keeping the class and constructor prevents R8 from stripping or renaming it.
-keep class ajstrick81.morphe.extension.peacock.ads.AdBlockInterceptor {
    public <init>();
}
