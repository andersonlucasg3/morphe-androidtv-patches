package ajstrick81.morphe.patches.youtubetv.ads

import app.morphe.patcher.Fingerprint

/**
 * Ad-related method fingerprints for YouTube Android TV
 * (com.google.android.youtube.tv v7.05.301).
 *
 * All strings are confirmed present in the decompiled APK DEX files.
 * Custom lambdas use structural checks only — no R8-obfuscated
 * class-name assumptions.
 *
 * Targeted ad delivery channels:
 * - Hook 1: InnerTube client OS spoofing
 * - Hook 2: VAST/VMAP video ad request short-circuit
 * - Hook 3: Ad break parser short-circuit (Disney pattern)
 * - Hook 4: Coroutine-based ad fetch stall (Tubi Hook 9 pattern)
 */

// Hook 1 — OS Name Spoofing
//
// Replaces the OS name in the InnerTube client context with
// "Android Automotive". YouTube does not serve video ads on
// Android Automotive (stricter automotive ad policies).
//
// DIAGNOSTIC: stripped to strings-only to identify which constraint
// (returnType or custom) is blocking the match. Will add back
// one at a time based on test results.
//
// Evidence: "ANDROID_TV" (3 DEX matches) confirmed in APK.
object YouTubeTvOsNameFingerprint : Fingerprint(
    strings = listOf("ANDROID_TV"),
)

// Hook 2 — VAST/VMAP Ad Request Short-Circuit
//
// Short-circuits the video ad request method. YouTube TV fetches
// ads via VAST/VMAP protocol (vastAdsRequest), not the slot/adapter
// system used in mobile YouTube. Setting a null/0 return prevents
// the ad manifest from being loaded.
//
// Evidence: "vastAdsRequest", "vmapAdsRequest" confirmed in APK.
//           Ad CDN strings ("googleads.g.doubleclick.net",
//           "pagead2.googlesyndication.com") also confirmed.
object YouTubeTvLoadVideoAdsFingerprint : Fingerprint(
    strings = listOf("vastAdsRequest"),
    custom = { method, _ ->
        // Structural: non-void ad fetch that is NOT a coroutine suspend function.
        method.returnType != "V" &&
            !method.parameterTypes.any { it.contains("Continuation") }
    },
)

// Hook 3 — Ad Break Parser Short-Circuit
//
// Finds the method that parses ad break data from the InnerTube
// player response protobuf. Returns an empty ArrayList to prevent
// any ad breaks from being scheduled.
//
// Pattern from Disney Insertion.getPoints / Insertion.getRanges.
//
// Evidence: "AdBreakClipInfo", "AdBreakStatus", "adBreak" (3 DEX
//           matches) all confirmed in APK.
object YouTubeTvAdBreakParserFingerprint : Fingerprint(
    strings = listOf("adBreak", "AdBreakClipInfo"),
    custom = { method, _ ->
        // Must return a List/Collection type to substitute empty ArrayList.
        val rt = method.returnType
        rt != null && (rt.contains("List") || rt.contains("Collection"))
    },
)

// Hook 4 — Coroutine Ad Fetch Stall
//
// Targets the Kotlin coroutine-based VAST/VMAP ad fetch method.
// Returns the COROUTINE_SUSPENDED sentinel to suspend the coroutine
// without completing it: the ad manifest never arrives, YouTube's
// timeout logic fires, and content plays without pre-roll ads.
//
// Pattern from Tubi Hook 9 (RainmakerSuspendGetAdBreaksFingerprint):
// suspend function with Continuation as last parameter, returning
// COROUTINE_SUSPENDED to stall the fetch.
//
// Evidence: "COROUTINE_SUSPENDED" and "vastAdsRequest" confirmed
//           in APK. Kotlin coroutines are used in YouTube TV.
object YouTubeTvSuspendGetAdsFingerprint : Fingerprint(
    strings = listOf("vastAdsRequest"),
    returnType = "Ljava/lang/Object;",
    custom = { method, _ ->
        // Must accept a Kotlin coroutine Continuation as last parameter
        // (the compiler-generated parameter of every suspend function).
        method.parameterTypes.any {
            it.contains("Continuation")
        }
    },
)
