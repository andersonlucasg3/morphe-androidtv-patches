/*
 * Paramount+ Android TV — Ad Patch Fingerprints
 *
 * Validated against:
 *   v16.8.0  (versionCode 520000464) — com.cbs.ott
 *   v16.12.0 (versionCode 520000571) — com.cbs.ott
 *
 * Three patch targets:
 *   1. createVodStreamRequest (3-arg) — primary SSAI DAI call site (yk0.run())
 *   2. createVodStreamRequest (4-arg) — secondary / fallback overload
 *   3. CbsPauseWithAdsOverlay state machine — pause ad overlay renderer
 *
 * Live TV (createLiveStreamRequest) is intentionally NOT patched.
 * The asset key / channel ID set by that method is required to obtain
 * the stitched manifest URL — patching it kills playback entirely.
 */

package app.morphe.patches.paramount

import app.morphe.patcher.Fingerprint

// ---------------------------------------------------------------------------
// Patch 1: createVodStreamRequest — 3-argument overload
//
// This is the overload called by yk0.run() (v16.12.0) / pk0.run() (v16.8.0),
// the obfuscated DAI state machine that drives SSAI stream requests for VOD.
//
// Smali (both versions — byte-for-byte identical):
//   new-instance v1, Lcom/google/ads/interactivemedia/v3/impl/zzcx;
//   sget-object  v0, Lcom/google/ads/interactivemedia/v3/internal/zzafv;->zzd
//   invoke-direct {v1, v0}, zzcx-><init>(zzafv)V
//   invoke-virtual {v1, v2}, zzcx->zze(String)V   ← contentSourceId (ad param)
//   invoke-virtual {v1, v3}, zzcx->zzf(String)V   ← videoId         (ad param)
//   invoke-virtual {v1, v4}, zzcx->zzo(String)V   ← apiKey          (ad param)
//   return-object v1
//
// Fingerprint strategy: ImaSdkFactory is fully unobfuscated (Google IMA SDK
// public API surface). Method name is stable. Parameter count disambiguates
// this overload from the 4-arg version.
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
// Patch 2: createVodStreamRequest — 4-argument overload
//
// Adds a 4th setter: zzcx->zzg(String) for networkCode.
// Called by the 5-arg StreamTrackingMode overload (which delegates here)
// and potentially direct callers outside the main DAI state machine.
//
// Fingerprint: identical strategy to the 3-arg, differentiated by param count.
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
// Patch 3: CbsPauseWithAdsOverlay — pause ad state machine
//
// This static method is the state machine dispatcher for the pause ad overlay.
// It receives a sealed state object and branches on its type:
//   - State carrying a URL  → Glide network image load → alpha fade-in to 1.0
//   - State carrying a File → Glide local image load   → alpha fade-in to 1.0
//   - Reset state           → calls O(0) → fade-out to alpha=0
//
// The method name is minified and drifts between versions:
//   v16.8.0  → P(CbsPauseWithAdsOverlay, uy1)V
//   v16.12.0 → M(CbsPauseWithAdsOverlay, lz1)V
//
// Fingerprint strategy: anchor on the two log strings emitted in the
// fallthrough branch — "renderState: " and " not updating overlay." —
// which are stable developer-facing log messages unlikely to change.
// The defining class name is partially unobfuscated (CBS/Paramount branded)
// and used as a secondary guard.
//
// Package path drifted between versions:
//   v16.8.0  → Lcom/cbs/player/view/tv/CbsPauseWithAdsOverlay;
//   v16.12.0 → Lcom/paramount/android/pplus/widgets/player/tv/view/CbsPauseWithAdsOverlay;
//
// Using endsWith() makes the fingerprint robust to future package migrations.
// ---------------------------------------------------------------------------

internal object PauseAdOverlayFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("renderState: ", " not updating overlay."),
    custom = { method, _ ->
        method.definingClass.endsWith("CbsPauseWithAdsOverlay;")
    },
)
