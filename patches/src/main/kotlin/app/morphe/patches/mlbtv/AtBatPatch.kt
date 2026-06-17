/*
 * MLB At Bat Android TV — Ad & Gambling Content Suppression Patch
 *
 * Validated against:
 *   v26.8.1  (versionCode 1750000022) — com.bamnetworks.mobile.android.gameday
 *
 * Coverage:
 *   ✅ VOD ads              — createVodStreamRequest() returns empty zzcx →
 *                             IMA SDK throws → fallback to pre-cached CDN URL
 *   ✅ Gambling ads (VOD)   — FanDuel, DraftKings, BetMGM via IMA SDK path
 *   ✅ Between-innings ads  — CreateMediaItemWithAdsUseCase blocked upstream →
 *                             DAI API and IMA SDK ad init both suppressed
 *   ➡️ Live games           — DAI untouched (createLiveStreamRequest path)
 *
 * SUSPEND FUNCTION STRATEGY:
 *   CreateMediaItemWithAdsUseCase is a Kotlin suspend function.
 *   It compiles to a method returning Ljava/lang/Object; with a Continuation
 *   parameter. return-void is invalid — instead inject:
 *     const/4 v0, 0x0
 *     return-object v0
 *   This returns null Object, completing the coroutine without executing
 *   the ad initialization body.
 *
 * NOTE ON PublicaAdBreakStartedFingerprint (REMOVED):
 *   The log string "[MlbMediaPlayer] onAdBreakStarted" lives in a different
 *   class than PublicaBidListener — the fingerprint never matched.
 *   CreateMediaItemWithAdsUseCase is the cleaner upstream intercept and has
 *   its own confirmed log strings directly in the method body.
 */

package app.morphe.patches.mlbtv

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val atbatPatch = bytecodePatch(
    name = "MLB At Bat Android TV",
    description = "Removes VOD ads, between-innings gambling ads, and sportsbook promotions while preserving live game playback.",
) {
    compatibleWith(AppCompatibilities.MLB_TV)

    execute {
        // ------------------------------------------------------------------
        // Patch 1: VOD SSAI & Gambling Content — createVodStreamRequest
        //
        // Returns a valid but empty zzcx StreamRequest → IMA SDK throws →
        // exception caught → fallback to pre-cached CDN URL (nm0.c).
        // Live games use createLiveStreamRequest() — separate path, untouched.
        // ------------------------------------------------------------------
        VodStreamRequest3ArgFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Lcom/google/ads/interactivemedia/v3/impl/zzcx;
                sget-object v1, Lcom/google/ads/interactivemedia/v3/internal/zzafv;->zzd:Lcom/google/ads/interactivemedia/v3/internal/zzafv;
                invoke-direct {v0, v1}, Lcom/google/ads/interactivemedia/v3/impl/zzcx;-><init>(Lcom/google/ads/interactivemedia/v3/internal/zzafv;)V
                return-object v0
            """.trimIndent(),
        )

        VodStreamRequest4ArgFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Lcom/google/ads/interactivemedia/v3/impl/zzcx;
                sget-object v1, Lcom/google/ads/interactivemedia/v3/internal/zzafv;->zzd:Lcom/google/ads/interactivemedia/v3/internal/zzafv;
                invoke-direct {v0, v1}, Lcom/google/ads/interactivemedia/v3/impl/zzcx;-><init>(Lcom/google/ads/interactivemedia/v3/internal/zzafv;)V
                return-object v0
            """.trimIndent(),
        )

        // ------------------------------------------------------------------
        // Patch 3: DAI/IMA Stream Init — CreateMediaItemWithAdsUseCase
        //
        // Upstream intercept for between-innings commercial breaks.
        // Controls both "Playing stream with DAI API" and "Playing stream
        // with IMA SDK" paths. Returning null Object (0x0) completes the
        // coroutine without initializing any ad stream.
        //
        // Confirmed unobfuscated in classes6.dex:
        //   mlb.atbat.media.player.ads.CreateMediaItemWithAdsUseCase
        //
        // If this causes live game issues, comment out this block only.
        // VOD patches above are independent and unaffected.
        // ------------------------------------------------------------------
        CreateMediaItemWithAdsFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
    }
}
