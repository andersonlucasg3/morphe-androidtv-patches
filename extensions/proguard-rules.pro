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
    public static *** isAdSegmentUrl(java.lang.String);
}
# Peacock — existing entry
# emptyAdPlaybackState is called reflectively by the Sky SDK layer patches.
-keep class ajstrick81.morphe.extension.peacock.ads.SkipAdsPatch {
    public static *** emptyAdPlaybackState(java.lang.Object);
}

# Peacock — Layer 6: OkHttp ad CDN interceptor
# AdBlockInterceptor is instantiated by PeacockAdPatchHelper at runtime.
# Keeping the class and no-arg constructor prevents R8 from stripping it.
-keep class ajstrick81.morphe.extension.peacock.ads.AdBlockInterceptor {
    public <init>();
}

# Peacock — Layer 6: method-replacement wrapper
# PeacockAdPatchHelper.buildOkHttpClient() is called directly from injected
# smali via invoke-static {}. R8 must not rename or remove this method.
# OkHttpWorkaroundInterceptor is also instantiated here — kept via its own
# existing rule elsewhere; confirm it has one if the build strips it.
-keep class ajstrick81.morphe.extension.peacock.ads.PeacockAdPatchHelper {
    public static okhttp3.OkHttpClient buildOkHttpClient();
}
