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
 *   ✅ Between-innings ads  — PublicaBidListener.onAdBreakStarted() blocked →
 *                             Publica auction cancelled → dclk_video_ads not fetched
 *   ✅ Publica bid upstream — GetPublicaBidsUseCase blocked (depth-of-defense)
 *   ➡️ Live games           — DAI untouched (createLiveStreamRequest, no fallback)
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
        // Returns a valid but empty zzcx StreamRequest. IMA SDK throws when
        // requestStream() is called with no parameters → exception caught →
        // ErrorCriticalEvent → fallback to pre-cached CDN URL (nm0.c).
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
        // Patch 3: Between-Innings Ad Break — PublicaBidListener
        //
        // Highest-level intercept for commercial breaks. Fires when DAI stream
        // signals a break. return-void cancels the entire chain:
        //   Publica auction → DAI pod metadata → googlevideo.com dclk_video_ads
        //
        // Confirmed via logcat:
        //   "[MlbMediaPlayer] onAdBreakStarted"
        //   googlevideo.com/.../source/dclk_video_ads (responseCode: 200)
        //
        // Expected: "Commercial Break - We'll be right back" shown instead of
        // BetMGM / Bet365 gambling ads.
        // ------------------------------------------------------------------
        PublicaAdBreakStartedFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        // ------------------------------------------------------------------
        // Patch 4: Publica Bid Upstream — GetPublicaBidsUseCase
        //
        // Depth-of-defense. Kills ad bid request before break even triggers.
        // NOTE: If this is a Kotlin suspend function the fingerprint won't
        // match — that is safe, Patch 3 handles the primary intercept.
        // If this causes a compile error, comment out these two lines only.
        // ------------------------------------------------------------------
        GetPublicaBidsFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
