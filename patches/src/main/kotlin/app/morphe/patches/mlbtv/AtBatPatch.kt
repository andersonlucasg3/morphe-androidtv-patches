/*
 * MLB At Bat Android TV — Ad & Gambling Content Suppression Patch
 *
 * Validated against:
 *   v26.8.1  (versionCode 1750000022) — com.bamnetworks.mobile.android.gameday
 *
 * Coverage:
 *   ✅ VOD ads              — createVodStreamRequest() empty zzdm →
 *                             IMA SDK throws → fallback to pre-cached CDN URL
 *   ✅ MLB EVI ads          — CONFIRMED BLOCKED (logcat 06-18: zero EVI segments)
 *                             ExoMediaPlayerMetadataFingerprint blocks TXXX dispatch
 *   ✅ SSAI media source    — Lb6/h;.b0() blocked → no SSAI startup →
 *                             requestStream() never called → no DAI manifest URL
 *   ✅ DAI StreamManager    — Lb6/h;.m0() blocked → no ad segment scheduling
 *   ✅ TXXX dispatch        — CONFIRMED BLOCKED (logcat: zero TXXX entries)
 *   ➡️ Live games           — Needs testing — monitor if b0()/m0() affect live DAI
 *
 * DIAGNOSIS BASED ON 06-18 LOGCAT:
 *   MLB EVI (/EVI/ segments): ZERO — confirmed blocked ✅
 *   TXXX metadata:            ZERO — confirmed blocked ✅
 *   dclk_video_ads:           22 segments — still fetching ❌
 *
 *   Root cause: Lb6/k;.b() returned empty zzdm but zzan.requestStream()
 *   succeeded anyway — IMA SDK uses server-side AdsLoader session state,
 *   not StreamRequest parameters, to generate the DAI manifest URL.
 *
 *   Fix: block Lb6/h;.b0() BEFORE requestStream() is called at all.
 */

package app.morphe.patches.mlbtv

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val atbatPatch = bytecodePatch(
    name = "MLB At Bat Android TV",
    description = "Removes VOD ads and between-innings gambling ads while preserving live game playback.",
) {
    compatibleWith(AppCompatibilities.MLB_TV)

    execute {
        // ------------------------------------------------------------------
        // Patch 1a: VOD SSAI — createVodStreamRequest (3-arg)
        // ------------------------------------------------------------------
        VodStreamRequest3ArgFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Lcom/google/ads/interactivemedia/v3/impl/zzdm;
                sget-object v1, Lcom/google/ads/interactivemedia/v3/internal/zzafs;->zzd:Lcom/google/ads/interactivemedia/v3/internal/zzafs;
                invoke-direct {v0, v1}, Lcom/google/ads/interactivemedia/v3/impl/zzdm;-><init>(Lcom/google/ads/interactivemedia/v3/internal/zzafs;)V
                return-object v0
            """.trimIndent(),
        )

        // ------------------------------------------------------------------
        // Patch 1b: VOD SSAI — createVodStreamRequest (4-arg)
        // ------------------------------------------------------------------
        VodStreamRequest4ArgFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Lcom/google/ads/interactivemedia/v3/impl/zzdm;
                sget-object v1, Lcom/google/ads/interactivemedia/v3/internal/zzafs;->zzd:Lcom/google/ads/interactivemedia/v3/internal/zzafs;
                invoke-direct {v0, v1}, Lcom/google/ads/interactivemedia/v3/impl/zzdm;-><init>(Lcom/google/ads/interactivemedia/v3/internal/zzafs;)V
                return-object v0
            """.trimIndent(),
        )

        // ------------------------------------------------------------------
        // Patch 2: SSAI MediaSource Startup — Lb6/h;.b0(Lq5/w;)V
        //
        // Verified: string="ImaServerSideAdInsertionMediaSource" (UNIQUE in APK)
        // proto=(Lq5/w;)V, registers=10
        //
        // Called when ImaServerSideAdInsertionMediaSource starts up.
        // return-void prevents: Lb6/h$g; construction → requestStream()
        // call → DAI manifest URL generation → dclk_video_ads segments.
        //
        // NOTE: If live games break, comment this patch out only.
        // Patches 1a/1b and 4 are independent and safe to keep.
        // ------------------------------------------------------------------
        SsaiMediaSourceStartupFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        // ------------------------------------------------------------------
        // Patch 3: DAI StreamManager Event Handler — Lb6/h;.m0(StreamManager)V
        //
        // Verified: strings="IMA DAI Stream Event: ", "GSTREAM:DAI"
        // Belt-and-suspenders: prevents StreamManager from processing DAI
        // stream and scheduling ad segments even if Patch 2 is bypassed.
        // ------------------------------------------------------------------
        DaiStreamManagerHandlerFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        // ------------------------------------------------------------------
        // Patch 4: TXXX Metadata Dispatcher — Lu70/i;.onMetadata(Ll5/t;)V
        //
        // CONFIRMED WORKING (logcat 06-18: zero TXXX, zero EVI segments).
        // Blocks ALL HLS timed metadata dispatch:
        //   → Lz70/b;.o() never called → MLB EVI coroutines never launched
        //   → Lb6/h$c;.onMetadata() never called → IMA cues suppressed
        // ------------------------------------------------------------------
        ExoMediaPlayerMetadataFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
