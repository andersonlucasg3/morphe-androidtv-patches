/*
 * Paramount+ Android TV — Ad Patch Fingerprints
 *
 * Validated against:
 *   v16.8.0  (versionCode 520000464) — com.cbs.ott
 *   v16.12.0 (versionCode 520000571) — com.cbs.ott
 *
 * Ad architecture confirmed via PCAPdroid + APK analysis:
 *
 *   PRE-ROLL  — CSAI, served from vod-gcs-cedexis.cbsaavideo.com (~3.4MB)
 *               Patched via CsaiAdRequestFingerprint + DNS rule.
 *
 *   MID-ROLL  — SSAI stitched into googlevideo.com manifest.
 *               dai.google.com fires per-break (~7-8KB at each cue point).
 *               Patched via AdStartedFingerprint + AdSkipFingerprint.
 *
 *   PAUSE ADS — CbsPauseWithAdsOverlay state machine.
 *               Patched via PauseAdOverlayFingerprint.
 *
 * REQUIRED AGH RULES (Onn 4K TV — com.cbs.ott):
 *   @@||pubads.g.doubleclick.net^$app=com.cbs.ott   ← IMA SDK init (load-bearing)
 *   @@||imasdk.googleapis.com^$app=com.cbs.ott       ← IMA SDK host
 *   @@||dai.google.com^$app=com.cbs.ott              ← DAI content manifest
 *   @@||cbs.hb-api.omtrdc.net^$app=com.cbs.ott       ← Adobe entitlement (load-bearing)
 *   ||cbsaavideo.com^                                 ← Pre-roll ad CDN (blockable)
 *   ||*.cbsaavideo.com^
 */

package app.morphe.patches.paramount

import app.morphe.patcher.Fingerprint

// ---------------------------------------------------------------------------
// Patch 1: Pause ads — CbsPauseWithAdsOverlay state machine
//
// Dispatcher for the pause ad overlay. Method name is minified and drifts:
//   v16.8.0  → P(CbsPauseWithAdsOverlay, uy1)V
//   v16.12.0 → M(CbsPauseWithAdsOverlay, lz1)V
//
// Anchored on stable fallthrough-branch log strings. endsWith() handles
// the package migration between v16.8.0 and v16.12.0:
//   v16.8.0  → Lcom/cbs/player/view/tv/CbsPauseWithAdsOverlay;
//   v16.12.0 → Lcom/paramount/android/pplus/widgets/player/tv/view/CbsPauseWithAdsOverlay;
// ---------------------------------------------------------------------------

internal object PauseAdOverlayFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("renderState: ", " not updating overlay."),
    custom = { method, _ ->
        method.definingClass.endsWith("CbsPauseWithAdsOverlay;")
    },
)

// ---------------------------------------------------------------------------
// Patch 2: Pre-roll CSAI — ImaSdkFactory.createAdsRequest()
//
// PCAPdroid confirmed pre-roll ad video is served from cbsaavideo.com
// (~3.4MB, +6.2s to +8.65s) — separate CDN from content (googlevideo.com).
// The CSAI state machine calls createAdsRequest() → sets ad tag URL →
// calls requestAds() → fetches from cbsaavideo.com.
//
// Returns a fresh AdsRequestImpl with no ad tag URL set. Paired with
// the DNS rule ||cbsaavideo.com^ for layered suppression.
//
// Fully unobfuscated (IMA SDK public API) — stable across all versions.
// ---------------------------------------------------------------------------

internal object CsaiAdRequestFingerprint : Fingerprint(
    returnType = "Lcom/google/ads/interactivemedia/v3/api/AdsRequest;",
    custom = { method, _ ->
        method.name == "createAdsRequest" &&
            method.definingClass ==
                "Lcom/google/ads/interactivemedia/v3/api/ImaSdkFactory;"
    },
)

// ---------------------------------------------------------------------------
// Patch 3a: Mid-roll SSAI — AD_STARTED handler (inject point)
//
// cl0.f(Ad) in v16.12.0 — the AD_STARTED event handler in Paramount's
// SSAI AdEventListener implementation (cl0), backed by the SSAI state
// holder dl0. Confirmed by anchor string "handleAdStarted: ".
//
// This method is the correct injection point because by the time it
// completes, all three conditions required by the skip method (g) are met:
//   dl0.i = true   — set by d() (AD_BREAK_STARTED) before f() runs
//   dl0.j = true   — set at the top of f()
//   dl0.h = ek0    — set mid-way through f() via b(Ad), contains ek0.c
//                     (ad duration ms, used by g() to compute seek target)
//
// Patch strategy: inject invoke-virtual {p0} ->skipMethod()V at position
// (instructions.size - 1), just before the final return-void. This calls
// the skip method after state is fully set, seeking past the ad segment.
// The skip method name is resolved at patch time from AdSkipFingerprint
// to remain stable across obfuscation changes.
//
// Register note: cl0.f() has .registers 7 with 2 params (this + Ad).
//   p0 register = registerCount - parameterCount = 7 - 2 = 5 → v5
//   Computed dynamically in ParamountPatch.kt to survive register changes.
// ---------------------------------------------------------------------------

internal object AdStartedFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("handleAdStarted: "),
    parameters = listOf("Lcom/google/ads/interactivemedia/v3/api/Ad;"),
)

// ---------------------------------------------------------------------------
// Patch 3b: Mid-roll SSAI — skip method (reference target)
//
// cl0.g() in v16.12.0 — the ad skip method. Anchored by the live stream
// guard string "skip ad is not supported for live streams", which is a
// stable developer-facing message unlikely to change.
//
// This fingerprint is NOT directly patched. It is used to dynamically
// resolve the obfuscated method name (e.g. "g") and defining class
// (e.g. "Lcl0;") at patch time, so the injection in AdStartedFingerprint
// does not hardcode either value.
//
// The method seeks the player to the end of the current ad:
//   iget-wide v2, dl0.h (ek0), Lek0;->c J  ← ad end time (ms)
//   invoke-virtual n1->q0(J)V               ← seek to ad end
// Works for VOD only — live stream guard fires first for linear content.
// ---------------------------------------------------------------------------

internal object AdSkipFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("skip ad is not supported for live streams"),
    custom = { method, _ ->
        method.parameterTypes.isEmpty()
    },
)
