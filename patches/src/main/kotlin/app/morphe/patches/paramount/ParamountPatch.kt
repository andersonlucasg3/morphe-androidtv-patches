/*
 * Paramount+ Android TV — Ad Suppression Patch
 *
 * Validated against:
 *   v16.8.0  (versionCode 520000464) — com.cbs.ott
 *   v16.12.0 (versionCode 520000571) — com.cbs.ott
 *
 * Coverage:
 *   ✅ VOD SSAI ads  — createVodStreamRequest() returns empty zzcx →
 *                      DAI fails → onAdError() forwarded unconditionally →
 *                      ErrorCriticalEvent → nm0.c-equivalent fallback →
 *                      content plays from cbsaavideo.com without SSAI ads
 *   ✅ Pause ads     — CbsPauseWithAdsOverlay state machine
 *   ➡️ Live TV       — DAI untouched (live TV has no fallback content URL)
 *
 * v16.12.0 REGRESSION (infinite spinner on VOD) + CROSS-VERSION FIX:
 *
 *   In v16.8.0, an empty zzcx StreamRequest caused requestStream() to throw
 *   synchronously, which was caught by the caller's try-catch and dispatched
 *   straight to the AVIA ErrorCriticalEvent handler — onAdError() was never
 *   reached for this particular failure.
 *
 *   In v16.12.0, the bundled IMA SDK's requestStream() no longer throws
 *   synchronously for an empty/invalid request — it validates asynchronously
 *   and reports the result later via the registered AdErrorListener. That
 *   listener's onAdError() callback only forwards the failure to the AVIA
 *   fallback when AdError.getErrorType() is LOAD or PLAY; any other error
 *   category (which is what the empty StreamRequest now produces under the
 *   v16.12 SDK's async validation) falls through to a silent return-void, so
 *   the fallback never fires and playback hangs forever.
 *
 *   Confirmed via direct bytecode comparison: the onAdError() LOAD/PLAY
 *   filter exists with an IDENTICAL instruction sequence in both v16.8.0
 *   (class Luk0;) and v16.12.0 (class Lcl0;) — only the obfuscated
 *   field/class names in the surrounding dispatch code differ per version.
 *   Patch 3 below deletes only the filter-check instructions and leaves the
 *   version-specific dispatch tail untouched, so it is correct for both
 *   versions without hardcoding any class names.
 */

package app.morphe.patches.paramount

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
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
        // videoId, or apiKey setters are called. Unchanged from v16.8.0 —
        // still required so that AVIA's fallback (Patch 3) has a failure to
        // react to in the first place.
        // ------------------------------------------------------------------
        arrayOf(
            VodStreamRequest3ArgFingerprint,
            VodStreamRequest4ArgFingerprint,
        ).forEach { fingerprint ->
            fingerprint.method.addInstructions(
                0,
                """
                    new-instance v0, Lcom/google/ads/interactivemedia/v3/impl/zzcx;
                    sget-object v1, Lcom/google/ads/interactivemedia/v3/internal/zzafv;->zzd:Lcom/google/ads/interactivemedia/v3/internal/zzafv;
                    invoke-direct {v0, v1}, Lcom/google/ads/interactivemedia/v3/impl/zzcx;-><init>(Lcom/google/ads/interactivemedia/v3/internal/zzafv;)V
                    return-object v0
                """.trimIndent(),
            )
        }

        // ------------------------------------------------------------------
        // Patch 3: AdErrorListener.onAdError() type filter removal.
        //
        // Confirmed present in BOTH v16.8.0 (class Luk0;) and v16.12.0
        // (class Lcl0;) — identical instruction sequence in both, only the
        // field/class names in the untouched dispatch tail differ per
        // version (Lvk0;/Lc1;/Lel0;/Lno9; in v16.8.0 vs.
        // Ldl0;/Ln1;/Lml0;/Lss7; in v16.12.0).
        //
        // Original body structure (indices, both versions identical):
        //   [0-3]   read AdError + getErrorType() into v0/v1
        //   [4]     sget-object AdErrorType.LOAD
        //   [5]     if-eq v1, v2 (LOAD) -> goto dispatch tail [14]
        //   [6-9]   re-read getErrorType() into v6
        //   [10]    sget-object AdErrorType.PLAY
        //   [11]    if-ne v6, v1 (not PLAY) -> goto [13]
        //   [12]    goto dispatch tail [14]
        //   [13]    return-void                          <- the bug
        //   [14+]   dispatch tail: build "DAI Ad Error '<code> <msg>'"
        //           string, forward to the AVIA ErrorCriticalEvent path
        //           (Lc1;->v0() / Ln1;->t0(), version-dependent name).
        //
        // In v16.8.0 this filter never affected the empty-StreamRequest
        // case because requestStream() threw synchronously and the fallback
        // fired via the caller's try-catch before onAdError was ever reached
        // for that failure. In v16.12.0 the synchronous throw no longer
        // happens, the failure is reported only through onAdError(), and the
        // empty-StreamRequest error apparently isn't classified as LOAD/PLAY
        // — so it falls into the silent return-void at index 13 and the
        // fallback never fires (the infinite spinner).
        //
        // Fix: delete indices 4-13 (the type checks + early return-void) so
        // every error type unconditionally falls through into the existing
        // dispatch tail at index 14. No class/field names are hardcoded, so
        // this is correct for both versions without modification.
        // ------------------------------------------------------------------
        OnAdErrorFingerprint.method.apply {
            repeat(10) { removeInstruction(4) }
        }

        // ------------------------------------------------------------------
        // Patch 2: Pause ads — CbsPauseWithAdsOverlay state machine
        //
        // Independent of IMA DAI. return-void prevents Glide image fetch,
        // alpha fade-in, and overlay render. Overlay stays at alpha=0.
        // Unchanged from v16.8.0.
        // ------------------------------------------------------------------
        PauseAdOverlayFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
