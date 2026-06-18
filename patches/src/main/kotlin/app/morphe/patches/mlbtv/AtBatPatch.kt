/*
 * MLB At Bat Android TV — Ad & Gambling Content Suppression Patch
 *
 * Validated against:
 *   v26.8.1  (versionCode 1750000022) — com.bamnetworks.mobile.android.gameday
 *
 * Coverage:
 *   ✅ VOD ads              — createVodStreamRequest() empty zzdm →
 *                             IMA SDK throws → fallback to pre-cached CDN URL
 *   ✅ Between-innings SSAI — Lb6/k;.b(Uri) empty zzdm →
 *                             ImaServerSideAdInsertionMediaSource fails
 *   ✅ MLB EVI ad segments  — Lu70/i;.onMetadata() blocked → Lz70/b;.o()
 *                             never called → EVI coroutines never launched
 *   ✅ Google DAI ad cues   — Lu70/i;.onMetadata() blocked → Lb6/h$c;
 *                             never called → dclk_video_ads never inserted
 *   ➡️ Live games           — Untouched (createLiveStreamRequest path)
 *   ➡️ Game stream          — Untouched (game .ts segments don't use TXXX path)
 *
 * PREVIOUS PATCH FAILURE (MlbTxxxAdCueFingerprint):
 *   Used "method.name != onMetadata" to distinguish Lz70/b;.o from
 *   Lb6/h$c;.onMetadata. This condition matched MANY empty stub delegates
 *   (registers=2, no logic) that also implement Ly70/s;->o(Ll5/t;)V.
 *   Morphe matched a stub → patch compiled and installed but did nothing.
 *
 * CORRECT APPROACH — SINGLE UPSTREAM PATCH:
 *   Lu70/i;.onMetadata is the ExoMediaPlayer listener that receives ALL
 *   HLS timed metadata from ExoPlayer and dispatches to all registered
 *   Ly70/s; listeners via invoke-interface. Patching this single method
 *   with return-void stops the entire TXXX chain before it reaches either
 *   Lz70/b;.o (MLB EVI) or Lb6/h$c;.onMetadata (IMA DAI).
 *
 * BYTECODE VERIFIED:
 *   StreamRequest impl:  Lcom/google/ads/interactivemedia/v3/impl/zzdm;
 *   VOD type constant:   Lcom/google/ads/interactivemedia/v3/internal/zzafs;->zzd
 *   zzdm constructor:    <init>(Lcom/google/ads/interactivemedia/v3/internal/zzafs;)V
 */

package app.morphe.patches.mlbtv

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val atbatPatch = bytecodePatch(
    name = "MLB At Bat Android TV",
    description = "Removes VOD ads and between-innings gambling ads while preserving live game and stream playback.",
) {
    compatibleWith(AppCompatibilities.MLB_TV)

    execute {
        // ------------------------------------------------------------------
        // Patch 1a: VOD SSAI — createVodStreamRequest (3-arg)
        // Empty zzdm → IMA SDK throws → fallback to pre-cached CDN URL.
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
        // Patch 2: Between-Innings SSAI — Lb6/k;.b(Uri)→StreamRequest
        //
        // Empty zzdm → SSAI source fails init → ExoPlayer fallback to plain HLS.
        // Verified: registers=8, p0=this, p1=Uri, v0=new zzdm, v1=zzafs type.
        // ------------------------------------------------------------------
        SsaiStreamRequestFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Lcom/google/ads/interactivemedia/v3/impl/zzdm;
                sget-object v1, Lcom/google/ads/interactivemedia/v3/internal/zzafs;->zzd:Lcom/google/ads/interactivemedia/v3/internal/zzafs;
                invoke-direct {v0, v1}, Lcom/google/ads/interactivemedia/v3/impl/zzdm;-><init>(Lcom/google/ads/interactivemedia/v3/internal/zzafs;)V
                return-object v0
            """.trimIndent(),
        )

        // ------------------------------------------------------------------
        // Patch 3: TXXX Metadata Dispatcher — Lu70/i;.onMetadata(Ll5/t;)V
        //
        // Verified: registers=5, name=onMetadata, proto=(Ll5/t;)V
        //   String: "[ExoMediaPlayer] metadata received from stream"
        //
        // Single upstream dispatcher for ALL HLS timed metadata.
        // return-void stops all downstream listener dispatch:
        //   → Lz70/b;.o() never called → MLB EVI coroutines never launched
        //   → Lb6/h$c;.onMetadata() never called → DAI cues never processed
        //   → tv-gmc.mlb.com/EVI/ and dclk_video_ads never dispatched
        // ------------------------------------------------------------------
        ExoMediaPlayerMetadataFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
