/*
 * MLB At Bat Android TV — Ad Patch Fingerprints
 *
 * Validated against:
 *   v26.8.1  (versionCode 1750000022) — com.bamnetworks.mobile.android.gameday
 *
 * PATCH COVERAGE:
 *
 *   Patch 1a/1b — VOD SSAI & Gambling Ads (createVodStreamRequest)
 *     Identical IMA SDK v3 as Paramount+ v16.8.0.
 *     Empty zzcx → IMA SDK throws → fallback to pre-cached CDN URL.
 *
 *   Patch 3 — DAI/IMA Stream Init (CreateMediaItemWithAdsUseCase)
 *     Confirmed in classes6.dex — unobfuscated class name, stable log strings:
 *       "[CreateMediaItemWithAdsUseCase] Playing stream with DAI API"
 *       "[CreateMediaItemWithAdsUseCase] Playing stream with IMA SDK"
 *     Sits upstream of both between-innings ad breaks AND VOD SSAI.
 *     Kotlin suspend function → returnType = "Ljava/lang/Object;"
 *     Return null Object to complete coroutine without executing body.
 *
 * REMOVED:
 *   PublicaAdBreakStartedFingerprint — log string "[MlbMediaPlayer] onAdBreakStarted"
 *   is not in the PublicaBidListener.onAdBreakStarted method body directly.
 *   It lives in a different class (MlbMediaPlayer or MlbPlayerComponent).
 *   Dropped in favour of CreateMediaItemWithAdsUseCase which is a cleaner
 *   upstream intercept confirmed by its own log strings.
 */

package app.morphe.patches.mlbtv

import app.morphe.patcher.Fingerprint

// ---------------------------------------------------------------------------
// Patch 1a: VOD SSAI & Gambling Ads — createVodStreamRequest (3-arg)
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
// Patch 3: DAI/IMA Stream Init — CreateMediaItemWithAdsUseCase
//
// Controls both DAI API and IMA SDK ad stream initialization paths for
// between-innings commercial breaks. Confirmed unobfuscated in classes6.dex:
//   mlb.atbat.media.player.ads.CreateMediaItemWithAdsUseCase
//
// Log strings confirmed in DEX:
//   "[CreateMediaItemWithAdsUseCase] Playing stream with DAI API"
//   "[CreateMediaItemWithAdsUseCase] Playing stream with IMA SDK"
//
// Kotlin suspend function → returnType = "Ljava/lang/Object;"
// Return null Object to complete coroutine without executing ad init body.
//
// NOTE: If this causes live game issues, it can be disabled — the VOD
// patches (1a/1b) are independent and do not depend on this fingerprint.
// ---------------------------------------------------------------------------

internal object CreateMediaItemWithAdsFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    strings = listOf(
        "[CreateMediaItemWithAdsUseCase] Playing stream with DAI API",
        "[CreateMediaItemWithAdsUseCase] Playing stream with IMA SDK",
    ),
    custom = { method, _ ->
        method.definingClass.endsWith("CreateMediaItemWithAdsUseCase;")
    },
)
