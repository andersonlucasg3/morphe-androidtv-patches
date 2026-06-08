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
# ProGuard rules for GMB Diagnostic extension.
# logGMBMessage is called directly from patched smali via invoke-static.
# Without this rule R8 may inline or remove it.
# TEMPORARY — remove when this diagnostic patch is replaced by production patch.

-keep class ajstrick81.morphe.extension.primevideo.gmb.GMBDiagnostic {
    public static *** logGMBMessage(java.lang.String, java.lang.String);
}
# ProGuard rules for Prime Video ATV extensions.
#
# All three methods are called directly from patched smali via invoke-static.
# Without these rules R8 may inline or remove them since they appear
# unreferenced from the extension module's own code graph.
#
# logGMBMessage is TEMPORARY — remove when the GMB diagnostic patch is
# replaced by the production suppression patch.

-keep class ajstrick81.morphe.extension.primevideo.ads.SkipAdsPatch {
    public static *** skipAllMedia3AdGroups(com.google.common.collect.ImmutableMap);
    public static *** skipAllExo2AdGroups(com.google.common.collect.ImmutableMap);
    public static *** logGMBMessage(java.lang.String, java.lang.String);
}
