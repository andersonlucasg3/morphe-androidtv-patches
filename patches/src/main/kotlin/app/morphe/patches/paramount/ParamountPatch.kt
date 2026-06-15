/*
 * Paramount+ Android TV — Ad Suppression Patch
 *
 * Validated against:
 *   v16.8.0  (versionCode 520000464) — com.cbs.ott
 *
 * Coverage:
 *   ✅ VOD SSAI ads  — createVodStreamRequest() returns empty zzcx →
 *                      IMA SDK throws → ErrorCriticalEvent → nm0.c fallback
 *                      → content plays from cbsaavideo.com without SSAI ads
 *   ✅ Pause ads     — CbsPauseWithAdsOverlay state machine
 *   ➡️ Live TV       — DAI untouched (live TV has no ek0.s fallback URL)
 *
 * EMPTY zzcx vs NULL:
 *   NULL return:      vk0.C = null → null guard fires at pk0.run()[163-164]
 *                     → silent retry loop → infinite spinner (NO fallback triggered)
 *
 *   EMPTY zzcx:       vk0.C = non-null (passes null guard) → requestStream()
 *                     called with unpopulated params → IMA SDK throws exception
 *                     → caught by pk0.run() try-catch → ErrorCriticalEvent
 *                     → AVIA error handler → nm0.c fallback fires ✅
 *
 * zzcx CONSTRUCTOR:
 *   In v16.8.0, zzcx is constructed in createVodStreamRequest() as:
 *     new-instance v0, Lcom/google/ads/interactivemedia/v3/impl/zzcx;
 *     sget-object v1, zzafv;->zzd (VOD integration type)
 *     invoke-direct v0, v1, zzcx;-><init>(zzafv)V
 *   We replicate this without calling the setter chain (zze/zzf/zzo).
 *   The object is valid but has no content parameters set.
 *
 *   Register note: .registers 5 for 3-arg (p0=this, p1..p3=strings).
 *   v0 = new zzcx, v1 = integration type constant. Safe to use both.
 */

package app.morphe.patches.paramount

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val paramountPatch = bytecodePatch(
    name = "Paramount+ Android TV",
    description = "Removes VOD ads and pause ads while preserving live TV.",
) {
    compatibleWith(AppCompatibilities.PARAMOUNT_TV)

    execute {
        // ------------------------------------------------------------------
        // Patch 1: VOD SSAI — createVodStreamRequest (3-arg and 4-arg)
        //
        // Returns a valid but empty zzcx StreamRequest. No contentSourceId,
        // videoId, or apiKey setters are called. When pk0.run() passes this
        // to requestStream(), the IMA SDK throws because the request has no
        // parameters. The exception is caught by pk0.run()'s try-catch and
        // dispatched as ErrorCriticalEvent.
        //
        // AVIA's ErrorCriticalEvent handler then checks nm0.c (built earlier
        // by g1.c() from ek0.s = cbsaavideo.com URL) and falls back to
        // direct playback from that URL — content without SSAI ads.
        //
        // Live TV is unaffected: it uses createLiveStreamRequest() which is
        // a completely separate code path in pk0.run() at instruction [147].
        //
        // zzcx constructor requires one parameter: zzafv integration type.
        // sget-object Lvk0;->K holds the factory but we use the static
        // integration type directly via zzafv->zzd (VOD mode).
        // ------------------------------------------------------------------
        arrayOf(
            VodStreamRequest3ArgFingerprint,
            VodStreamRequest4ArgFingerprint,
        ).forEach { fingerprint ->
            fingerprint.method.addInstructions(
                0,
                """
                    new-instance v0, Lcom/google/ads/interactivemedia/v3/impl/zzcx;
                    sget-object v1, Lcom/google/ads/interactivemedia/v3/internal/zzafv;->zzd Lcom/google/ads/interactivemedia/v3/internal/zzafv;
                    invoke-direct {v0, v1}, Lcom/google/ads/interactivemedia/v3/impl/zzcx;-><init>(Lcom/google/ads/interactivemedia/v3/internal/zzafv;)V
                    return-object v0
                """.trimIndent(),
            )
        }

        // ------------------------------------------------------------------
        // Patch 2: Pause ads — CbsPauseWithAdsOverlay state machine
        //
        // Independent of IMA DAI. return-void prevents Glide image fetch,
        // alpha fade-in, and overlay render. Overlay stays at alpha=0.
        // ------------------------------------------------------------------
        PauseAdOverlayFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
