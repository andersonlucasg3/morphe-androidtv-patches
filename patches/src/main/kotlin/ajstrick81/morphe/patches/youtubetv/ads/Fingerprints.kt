package ajstrick81.morphe.patches.youtubetv.ads

import app.morphe.patcher.Fingerprint

/**
 * Ad-related method fingerprints for YouTube Android TV
 * (com.google.android.youtube.tv v7.05.301).
 *
 * All strings confirmed present in APK v7.05.301 DEX.
 * All hooks exclude <clinit> and <init> to prevent injecting
 * into static/instance initializers (causes VerifyError at
 * class load time — instant crash on app startup).
 */

// Hook 1 — OS Name Spoofing
//
// Replaces the OS name in the InnerTube client context with
// "Android Automotive". YouTube does not serve video ads on
// Android Automotive (stricter automotive ad policies).
//
// Evidence: "ANDROID_TV" (3 DEX matches) confirmed in APK.
object YouTubeTvOsNameFingerprint : Fingerprint(
    strings = listOf("ANDROID_TV"),
    custom = { method, _ ->
        method.name != "<clinit>" && method.name != "<init>"
    },
)

// Hook 2 — VAST/VMAP Ad Request Short-Circuit
//
// Short-circuits the video ad request method. YouTube TV fetches
// ads via VAST/VMAP protocol, not the slot/adapter system used
// in mobile YouTube.
//
// Evidence: "vastAdsRequest", "vmapAdsRequest" confirmed in APK.
object YouTubeTvLoadVideoAdsFingerprint : Fingerprint(
    strings = listOf("vastAdsRequest"),
    custom = { method, _ ->
        method.name != "<clinit>" && method.name != "<init>" &&
            method.returnType != "V" &&
            !method.parameterTypes.any { it.contains("Continuation") }
    },
)

// Hook 3 — Ad Break Parser Short-Circuit
//
// Finds the method that parses ad break data. Returns an empty
// ArrayList to prevent ad breaks from being scheduled.
// Pattern from Disney Insertion.getPoints / Insertion.getRanges.
//
// Evidence: "AdBreakClipInfo" confirmed in APK v7.05.301.
object YouTubeTvAdBreakParserFingerprint : Fingerprint(
    strings = listOf("AdBreakClipInfo"),
    custom = { method, _ ->
        method.name != "<clinit>" && method.name != "<init>"
    },
)

// Hook 4 — Coroutine Ad Fetch Stall
//
// Targets the VMAP ad fetch method. Returns COROUTINE_SUSPENDED
// sentinel to stall the ad manifest coroutine.
// Pattern from Tubi Hook 9.
//
// Evidence: "vmapAdsRequest" confirmed in APK v7.05.301.
object YouTubeTvSuspendGetAdsFingerprint : Fingerprint(
    strings = listOf("vmapAdsRequest"),
    custom = { method, _ ->
        method.name != "<clinit>" && method.name != "<init>"
    },
)
