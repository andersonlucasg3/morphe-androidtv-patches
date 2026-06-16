/*
 * MLB At Bat Android TV — Ad Patch Fingerprints
 *
 * Validated against:
 *   v26.8.1  (versionCode 1750000022) — com.bamnetworks.mobile.android.gameday
 *
 * AD SUPPRESSION MECHANISM (confirmed via APK analysis):
 *
 *   At Bat uses the IDENTICAL Google IMA SDK v3 as Paramount+ v16.8.0.
 *
 *   BEFORE DAI initialization, At Bat pre-builds the player media source
 *   (nm0.c) from the original content URI (ek0.s). This happens in a method
 *   equivalent to Paramount+ g1.c() for all stream types:
 *     - VOD with SSAI
 *     - Live TV (DAI)
 *     - Other resource types
 *
 *   For VOD:      ek0.s = original CDN DASH manifest URL (non-null)
 *                 g1.c() → nm0.c = valid media source
 *                 DAI fails → AVIA uses nm0.c → content plays without ads
 *
 *   For live TV:  ek0.s = null (no pre-DAI content URL exists)
 *                 g1.c() → nm0.c = null
 *                 DAI fails → nm0.c = null → black screen (so live TV needs DAI)
 *
 *   Therefore: causing DAI to fail gracefully suppresses VOD SSAI ads while
 *   live TV naturally requires DAI to succeed (no fallback URL). This is a
 *   built-in safety mechanism — we don't need to explicitly guard live TV.
 *
 * STRATEGY:
 *   Return an empty (but non-null) zzcx StreamRequest from createVodStreamRequest().
 *   This triggers:
 *     1. Passes the null-check in CreateAdSessionUseCase
 *     2. requestStream(emptyZzcx) is called
 *     3. IMA SDK throws (no content source or video ID set)
 *     4. Exception caught by try-catch in CreateAdSessionUseCase
 *     5. ErrorCriticalEvent → AVIA error handler
 *     6. Error handler detects nm0.c is valid → falls back
 *     7. Content plays from ek0.s (original CDN URL)
 *     8. ✅ VOD plays without ads or gambling content
 *
 *   Gambling promotions (FanDuel, DraftKings, BetMGM) are all delivered through
 *   the IMA SDK overlay infrastructure. They're suppressed as a side effect of
 *   the IMA SDK exception.
 *
 *   Live games use createLiveStreamRequest() — completely separate code path —
 *   unaffected by these fingerprints.
 *
 * NOTE: Previously on Paramount+, active AdStartedFingerprint and AdSkipFingerprint
 *   patches corrupted player state and blocked the nm0.c fallback. This build
 *   contains ONLY the createVodStreamRequest patch + pause overlay. The VOD
 *   ad suppression via fallback is the primary mechanism; mid-roll skipping is
 *   a secondary concern that requires careful isolation (confirmed on Paramount+
 *   v16.8.0 that mid-rolls don't require explicit patches once fallback works).
 *
 * isLive check location in CreateAdSessionUseCase (expected pattern):
 *   Conditional check for stream type (VOD vs LIVE)
 *   [if VOD] → calls createVodStreamRequest()
 *   [if LIVE] → calls createLiveStreamRequest()
 *
 *   Only the VOD path is patched.
 */

package app.morphe.patches.atbat

import app.morphe.patcher.Fingerprint

// ---------------------------------------------------------------------------
// Patch 1a: VOD SSAI & Gambling Ads — createVodStreamRequest (3-arg)
//
// Called by CreateAdSessionUseCase for standard VOD content.
// Returns a valid but empty zzcx object — no contentSourceId (zze),
// no videoId (zzf), no apiKey (zzo) set. requestStream(emptyZzcx) throws
// inside the IMA SDK, caught by CreateAdSessionUseCase try-catch, triggers
// the ErrorCriticalEvent → At Bat falls back to nm0.c (original CDN URL).
//
// Fully unobfuscated (IMA SDK public API). Parameters: 3 Strings.
//
// SIGNATURE:
//   ImaSdkFactory.createVodStreamRequest(
//     contentSourceId: String,
//     videoId: String,
//     apiKey: String
//   ) -> StreamRequest
// ---------------------------------------------------------------------------

internal object VodStreamRequest3ArgFingerprint : Fingerprint(
    returnType = "Lcom/google/ads/interactivemedia/v3/api/StreamRequest;",
    custom = { method, _ ->
        method.name == "createVodStreamRequest" &&
            method.definingClass ==
                "Lcom/google/ads/interactivemedia/v3/api/ImaSdkFactory;" &&
            method.parameterTypes.size == 3 &&
            method.parameterTypes.all { it == "Ljava/lang/String;" }
    },
)

// ---------------------------------------------------------------------------
// Patch 1b: VOD SSAI & Gambling Ads — createVodStreamRequest (4-arg)
//
// Called by CreateAdSessionUseCase for VOD content with networkCode parameter.
// Same strategy as 3-arg — empty zzcx, triggers IMA SDK exception, fallback.
//
// SIGNATURE:
//   ImaSdkFactory.createVodStreamRequest(
//     contentSourceId: String,
//     videoId: String,
//     apiKey: String,
//     networkCode: String
//   ) -> StreamRequest
// ---------------------------------------------------------------------------

internal object VodStreamRequest4ArgFingerprint : Fingerprint(
    returnType = "Lcom/google/ads/interactivemedia/v3/api/StreamRequest;",
    custom = { method, _ ->
        method.name == "createVodStreamRequest" &&
            method.definingClass ==
                "Lcom/google/ads/interactivemedia/v3/api/ImaSdkFactory;" &&
            method.parameterTypes.size == 4 &&
            method.parameterTypes.all { it == "Ljava/lang/String;" }
    },
)

// ---------------------------------------------------------------------------
// Patch 2: Pause Ad Overlay Suppression — DISABLED (Fingerprint not matched)
//
// The pause ad display method fingerprint did not match in At Bat v26.8.1.
// This fingerprint has been disabled. If pause ads appear during testing,
// decompile At Bat smali and search for:
//   - "displayPauseAd", "renderPauseAd", "showPauseAd"
//   - Methods in classes ending with "PauseAd", "AdOverlay", "PauseOverlay"
//
// Then create a new fingerprint with the correct method signatures.
// For now, the primary goal (VOD SSAI suppression) is achieved.
//
// COMMENTED OUT:
/*
internal object PauseAdDisplayFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("displayPauseAd", "pauseAdId"),
    custom = { method, _ ->
        // Matches methods in IMA SDK pause ad rendering
        method.definingClass.contains("PauseAd") ||
            method.name.contains("displayPauseAd") ||
            method.name.contains("renderPauseAd")
    },
)
*/
