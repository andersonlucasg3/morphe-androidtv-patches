/*
 * MLB At Bat Android TV — Ad Patch Fingerprints
 *
 * Validated against:
 *   v26.8.1  (versionCode 1750000022) — com.bamnetworks.mobile.android.gameday
 *
 * ALL FINGERPRINTS VERIFIED via full APK bytecode analysis (androguard).
 *
 * AD DELIVERY ARCHITECTURE (confirmed via logcat + bytecode):
 *
 *   Between-innings ads use TWO delivery systems on the SAME domain:
 *     Game content: tv-gmc.mlb.com/{date}/{gameId}-HD_7500K/.../{seg}.ts
 *     Ad content:   tv-gmc.mlb.com/EVI/{date}/{gameId}-AD-evi_7500K/.../{seg}.ts
 *
 *   DNS blocking cannot distinguish paths — domain blocking kills the stream.
 *   The /EVI/ ad segments are scheduled via HLS TXXX timed metadata cues
 *   embedded in the DAI manifest. Two handlers process these cues:
 *
 *   Patch 3 — Lz70/b;.o(Ll5/t;)V  [MLB-SPECIFIC TXXX HANDLER]
 *     Reads TXXX cue from HLS stream, parses ad break timing, launches
 *     coroutines Lz70/d; and Lz70/e; to fetch pod metadata and dispatch
 *     EVI segment URLs to ExoPlayer. Also fires tracker_ad_impression event.
 *     return-void here → ExoPlayer never receives EVI segment URLs →
 *     game stream plays straight through the break.
 *     VERIFIED: registers=15, only string = "TXXX", param = Ll5/t;
 *     UNIQUE: Only 1 method in APK with TXXX + Ll5/t; + registers=15
 *
 *   Patch 4 — Lb6/h$c;.onMetadata(Ll5/t;)V  [IMA SSAI TXXX HANDLER]
 *     IMA SSAI layer handler — calls onUserTextReceived() on all registered
 *     VideoStreamPlayerCallback instances with the TXXX payload. This is
 *     what triggers Google DAI segment insertion alongside MLB EVI.
 *     return-void here → IMA SSAI never processes the ad cue →
 *     dclk_video_ads segments also suppressed at the cue level.
 *     VERIFIED: registers=11, string = "TXXX", param = Ll5/t;
 *     UNIQUE: Only TXXX handler in Lb6/h$c; class
 *
 *   Together Patches 3+4 suppress BOTH ad systems at the HLS cue level —
 *   before any segment URLs reach ExoPlayer. This is the correct layer
 *   since DNS path-filtering is impossible on tv-gmc.mlb.com.
 *
 * IMA SDK StreamRequest implementation (confirmed from createVodStreamRequest):
 *   Class:       Lcom/google/ads/interactivemedia/v3/impl/zzdm;
 *   Constructor: <init>(Lcom/google/ads/interactivemedia/v3/internal/zzafs;)V
 *   VOD type:    Lcom/google/ads/interactivemedia/v3/internal/zzafs;->zzd
 */

package app.morphe.patches.mlbtv

import app.morphe.patcher.Fingerprint

// ---------------------------------------------------------------------------
// Patch 1a: VOD SSAI — createVodStreamRequest (3-arg)
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
// Patch 1b: VOD SSAI — createVodStreamRequest (4-arg)
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
// VERIFIED: (Landroid/net/Uri;)Lcom/google/ads/interactivemedia/v3/api/StreamRequest;
// Registers: 8. Strings: "ssai", "dai.google.com", "assetKey",
//   "Invalid URI scheme or authority." (17 total, all unique to this method)
// Parses ssai://dai.google.com URIs for ImaServerSideAdInsertionMediaSource.
// Empty zzdm → SSAI source fails init → ExoPlayer falls back to plain HLS.
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

// ---------------------------------------------------------------------------
// Patch 3: MLB TXXX Ad Cue Handler — Lz70/b;.o(Ll5/t;)V
//
// VERIFIED: (Ll5/t;)V, registers=15, only string in body = "TXXX"
// UNIQUE: Only 1 method in entire APK matches TXXX + Ll5/t; + registers=15
//
// MLB-specific HLS timed metadata handler. Reads TXXX cue from DAI
// manifest, parses ad break timing, launches coroutines Lz70/d; and Lz70/e;
// to fetch pod metadata and schedule tv-gmc.mlb.com/EVI/ segment URLs.
// return-void cancels the entire MLB EVI ad dispatch chain.
// ---------------------------------------------------------------------------

internal object MlbTxxxAdCueFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("TXXX"),
    custom = { method, _ ->
        method.parameterTypes.size == 1 &&
            method.parameterTypes[0] == "Ll5/t;" &&
            method.getCode()?.registersSize == 15
    },
)

// ---------------------------------------------------------------------------
// Patch 4: IMA SSAI TXXX Handler — Lb6/h$c;.onMetadata(Ll5/t;)V
//
// VERIFIED: (Ll5/t;)V, registers=11, string = "TXXX"
// UNIQUE: Only TXXX handler in class Lb6/h$c; (ImaSSAI inner listener)
//
// IMA SSAI layer handler. Calls onUserTextReceived() on all registered
// VideoStreamPlayerCallback instances with TXXX payload — this triggers
// Google DAI (dclk_video_ads) segment insertion. return-void suppresses
// the IMA SSAI ad cue processing alongside MLB EVI (Patch 3).
// ---------------------------------------------------------------------------

internal object ImaSsaiTxxxHandlerFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("TXXX"),
    custom = { method, _ ->
        method.name == "onMetadata" &&
            method.definingClass.contains("b6/h") &&
            method.parameterTypes.size == 1 &&
            method.parameterTypes[0] == "Ll5/t;" &&
            method.getCode()?.registersSize == 11
    },
)
