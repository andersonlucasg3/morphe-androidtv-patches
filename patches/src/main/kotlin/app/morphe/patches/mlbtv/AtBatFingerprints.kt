/*
 * MLB At Bat Android TV — Ad Patch Fingerprints
 *
 * Validated against:
 *   v26.8.1  (versionCode 1750000022) — com.bamnetworks.mobile.android.gameday
 *
 * ALL FINGERPRINTS VERIFIED via full APK bytecode analysis (androguard).
 * All class names, method signatures, register counts, and string refs confirmed.
 *
 * IMA SDK StreamRequest implementation in this APK version:
 *   Class:       Lcom/google/ads/interactivemedia/v3/impl/zzdm;
 *   Constructor: <init>(Lcom/google/ads/interactivemedia/v3/internal/zzafs;)V
 *   VOD type:    Lcom/google/ads/interactivemedia/v3/internal/zzafs;->zzd
 *   Live type:   Lcom/google/ads/interactivemedia/v3/internal/zzafs;->zzc
 *
 * NOTE: Earlier patch iterations used zzcx/zzafv — those classes still exist
 * in the APK but serve different purposes (PauseAd and unrelated types).
 * The correct StreamRequest impl is zzdm with zzafs integration type.
 *
 * BETWEEN-INNINGS AD ROOT CAUSE (confirmed via logcat + bytecode):
 *   Ads are delivered via Media3 ImaServerSideAdInsertionMediaSource (Lb6/h;).
 *   The DAI HLS manifest (dai.google.com/linear/hls/...) has ad segments
 *   server-side stitched in BEFORE ExoPlayer plays. All previous event-level
 *   patches (onAdEvent, onAdBreakStarted, etc.) fired AFTER ExoPlayer was
 *   already playing the ad — wrong layer.
 *
 *   Correct intercept: Lb6/k;.b(Uri)→StreamRequest
 *   This is where ssai://dai.google.com URIs are parsed into StreamRequests
 *   for ImaServerSideAdInsertionMediaSource. Return empty zzdm here and
 *   the SSAI media source fails to init → ExoPlayer falls back to plain HLS.
 */

package app.morphe.patches.mlbtv

import app.morphe.patcher.Fingerprint

// ---------------------------------------------------------------------------
// Patch 1a: VOD SSAI — createVodStreamRequest (3-arg)
//
// VERIFIED bytecode:
//   new-instance v0, Lcom/google/ads/interactivemedia/v3/impl/zzdm;
//   sget-object  v1, zzafs;->zzd (VOD type)
//   invoke-direct v0, v1, zzdm;-><init>(zzafs)V
//   invoke-virtual v0, v3, zzdm;->zze(String)V  [contentSourceId]
//   invoke-virtual v0, v4, zzdm;->zzf(String)V  [videoId]
//   invoke-virtual v0, v5, zzdm;->zzo(String)V  [apiKey]
//   return-object v0
//
// Our patch: return empty zzdm (no setters) → IMA SDK throws → fallback.
// .registers 6, p0=this, p1..p3=strings, v0=new zzdm, v1=zzafs type.
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
// Patch 1b: VOD SSAI — createVodStreamRequest (4-arg)
// Same approach, extra networkCode String parameter.
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
// Patch 2: Between-Innings SSAI — Lb6/k;.b(Uri)→StreamRequest
//
// VERIFIED bytecode (Lb6/k;.b):
//   Proto:     (Landroid/net/Uri;)Lcom/google/ads/interactivemedia/v3/api/StreamRequest;
//   Return:    Lcom/google/ads/interactivemedia/v3/api/StreamRequest;
//   Params:    Landroid/net/Uri; (p1)
//   Registers: 8
//   Strings:   "ssai", "dai.google.com", "assetKey", "apiKey",
//              "contentSourceId", "videoId", "networkCode", "format",
//              "adTagParameters", "manifestSuffix", "contentUrl",
//              "authToken", "streamActivityMonitorId",
//              "customUiOptionsSkippableSupport",
//              "customUiOptionsAboutThisAdSupport",
//              "Unsupported stream format:", "Invalid URI scheme or authority."
//
// This method parses ssai://dai.google.com URIs for
// ImaServerSideAdInsertionMediaSource (Lb6/h;). Returning an empty
// zzdm StreamRequest here causes the SSAI media source to fail
// initialization → ExoPlayer falls back to plain HLS without ads.
//
// Callers confirmed:
//   Lb6/h$d;.b(MediaSource)  — SSAI media source factory
//   Lb6/h;.<init>(...)        — SSAI media source constructor
//
// Uses unique combination of Uri parameter + "ssai"/"dai.google.com" strings.
// ---------------------------------------------------------------------------

internal object SsaiStreamRequestFingerprint : Fingerprint(
    returnType = "Lcom/google/ads/interactivemedia/v3/api/StreamRequest;",
    strings = listOf(
        "ssai",
        "dai.google.com",
        "assetKey",
        "Invalid URI scheme or authority.",
    ),
    custom = { method, _ ->
        method.parameterTypes.size == 1 &&
            method.parameterTypes[0] == "Landroid/net/Uri;"
    },
)
