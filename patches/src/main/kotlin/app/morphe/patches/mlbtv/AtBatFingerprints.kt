/*
 * MLB At Bat Android TV — Ad Patch Fingerprints
 *
 * Validated against:
 *   v26.8.1  (versionCode 1750000022) — com.bamnetworks.mobile.android.gameday
 *
 * ALL FINGERPRINTS VERIFIED via full APK bytecode analysis (androguard).
 *
 * BETWEEN-INNINGS AD ARCHITECTURE (confirmed via bytecode trace):
 *
 *   IMA SDK fires AdEvent → Lv70/k;.onAdEvent(AdEvent)V
 *     ↓ dispatches "Ad Break Started" → calls Lv70/k;.e() (onAdStarted)
 *     ↓ dispatches "Ad Period Started" → calls Lv70/k;.b() (onAdBreakStarted)  ← WAS PATCHING HERE (too late)
 *     ↓ IMA SDK already scheduled googlevideo.com segments upstream
 *
 *   CORRECT intercept: Lv70/k;.onAdEvent(AdEvent)V
 *     return-void here prevents ALL ad event dispatching before any
 *     segment scheduling occurs.
 *
 *   SECONDARY intercept: Lz70/i;.z()V (pod metadata timer)
 *     Called by Lz70/i;.s()V and Lz70/i;.r(...)V
 *     Prevents DAI pod metadata fetch → no segment URLs generated
 *
 * PREVIOUS PATCH FAILURE REASON:
 *   Lv70/k;.b() logs "[MlbMediaPlayer] onAdBreakStarted" but is called
 *   AFTER IMA SDK has already dispatched the ad break and scheduled
 *   googlevideo.com dclk_video_ads segment downloads. Patching b() had
 *   no effect on ad playback because the segments were already en route.
 */

package app.morphe.patches.mlbtv

import app.morphe.patcher.Fingerprint

// ---------------------------------------------------------------------------
// Patch 1a: VOD SSAI & Gambling Ads — createVodStreamRequest (3-arg)
// Unobfuscated IMA SDK public API — confirmed present in APK.
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
// Patch 3: IMA SDK Ad Event Dispatcher — Lv70/k;.onAdEvent
//
// VERIFIED: Class Lv70/k; method onAdEvent(AdEvent)V
// This is the IMA SDK AdEvent listener — the true upstream entry point
// for ALL between-innings ad events including "Ad Break Started".
//
// return-void here prevents all ad event dispatching before IMA SDK
// can schedule googlevideo.com dclk_video_ads segment downloads.
//
// Unique strings confirmed in bytecode:
//   "Ad Break Started", "Ad Period Started", "Ad Completed",
//   "Ad Buffering", "Ad Midpoint", "GSTREAM:DAI"
//
// Uses unobfuscated IMA SDK parameter type for precise matching.
// ---------------------------------------------------------------------------

internal object AdEventListenerFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(
        "Ad Break Started",
        "Ad Period Started",
        "GSTREAM:DAI",
    ),
    custom = { method, _ ->
        method.name == "onAdEvent" &&
            method.parameterTypes.size == 1 &&
            method.parameterTypes[0] ==
                "Lcom/google/ads/interactivemedia/v3/api/AdEvent;"
    },
)

// ---------------------------------------------------------------------------
// Patch 4: DAI Pod Metadata Timer — Lz70/i;.z()V
//
// VERIFIED: Class Lz70/i; method z()V
// No parameters, single string ref: "[LinearGoogleDaiListener] Starting pod metadata timer"
// Called by Lz70/i;.s()V and Lz70/i;.r(...)V
// Prevents DAI pod metadata fetch → no ad segment URLs generated.
// Depth-of-defense below Patch 3.
// ---------------------------------------------------------------------------

internal object LinearDaiPodMetadataFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("[LinearGoogleDaiListener] Starting pod metadata timer"),
    custom = { method, _ ->
        method.parameterTypes.isEmpty()
    },
)
