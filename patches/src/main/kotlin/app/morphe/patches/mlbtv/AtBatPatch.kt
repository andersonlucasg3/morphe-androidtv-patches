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
 *   ✅ Between-innings ads  — onAdEvent(AdEvent) blocked → IMA SDK cannot
 *                             dispatch "Ad Break Started" or schedule any
 *                             googlevideo.com dclk_video_ads segments
 *   ✅ DAI pod segments     — LinearGoogleDaiListener pod metadata timer
 *                             blocked (depth-of-defense)
 *   ➡️ Live games           — Needs testing; onAdEvent is a live path too.
 *                             If live games break, comment out Patch 3 only.
 *
 * WHY PREVIOUS PATCH FAILED:
 *   Lv70/k;.b() "[MlbMediaPlayer] onAdBreakStarted" is called by onAdEvent
 *   AFTER IMA SDK has already scheduled the ad segments. Patching b() was
 *   too late in the chain — segments were already en route to the player.
 *   onAdEvent() is the correct upstream intercept.
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
        // Returns valid but empty zzcx StreamRequest → IMA SDK throws →
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
        // Patch 3: IMA SDK Ad Event Dispatcher — onAdEvent(AdEvent)V
        //
        // Verified: Lv70/k;.onAdEvent(AdEvent)V
        // This is the IMA SDK AdEvent listener — true upstream entry point
        // for ALL between-innings ad events.
        //
        // return-void prevents "Ad Break Started" dispatch and all
        // downstream googlevideo.com dclk_video_ads segment scheduling.
        //
        // NOTE: If live games are affected, comment out this patch only.
        // The VOD patches above are independent and unaffected.
        // ------------------------------------------------------------------
        AdEventListenerFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        // ------------------------------------------------------------------
        // Patch 4: DAI Pod Metadata Timer — Lz70/i;.z()V
        //
        // Verified: no parameters, single string ref in body.
        // Depth-of-defense: prevents DAI pod metadata fetch even if
        // Patch 3 is bypassed by a different ad event code path.
        // ------------------------------------------------------------------
        LinearDaiPodMetadataFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
