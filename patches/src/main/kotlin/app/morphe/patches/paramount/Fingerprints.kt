/*
 * Paramount+ Android TV — Ad Patch Fingerprints
 *
 * Validated against:
 *   v16.8.0  (versionCode 520000464) — com.cbs.ott  [original]
 *   v16.12.0 (versionCode 520000571) — com.cbs.ott  [adds OnAdErrorFingerprint]
 *
 * AD SUPPRESSION MECHANISM (confirmed via APK analysis of v16.8.0 and v16.12.0):
 *
 *   g1.c() builds the player media source (nm0.c) from ek0.s (content URI)
 *   BEFORE DAI initialization begins. This runs for all resource config types.
 *
 *   For VOD:      content URL is non-null → fallback media source is valid.
 *   For live TV:  no pre-DAI content URL exists → fallback media source is null.
 *
 *   STRATEGY: return an empty (but non-null) zzcx StreamRequest from
 *   createVodStreamRequest(). This is unchanged and still required.
 *
 * v16.12.0 REGRESSION (new in this build, root cause of the infinite spinner):
 *
 *   The bundled IMA SDK's zzah.requestStream() no longer synchronously throws
 *   on an empty/invalid StreamRequest — it either re-dispatches a cached
 *   AdErrorEvent or posts the request to an async Executor, and reports
 *   success/failure later via the registered AdErrorListener/AdsLoadedListener
 *   (Lcl0; in v16.12, registered in Lyk0;->run()).
 *
 *   Lcl0;->onAdError(AdErrorEvent) — the callback that is supposed to forward
 *   the failure into the AVIA fallback path (Ln1;->t0(Boolean, Lml0)) — only
 *   does so when AdError.getErrorType() is LOAD or PLAY. Any other error
 *   category (which is what the empty StreamRequest now triggers under the
 *   v16.12 IMA SDK's async validation) falls through to a silent return-void.
 *   The fallback (nm0.c / direct cbsaavideo.com playback) never fires →
 *   infinite spinner, no content.
 *
 *   FIX: patch onAdError() to forward ALL error types unconditionally to
 *   Ln1;->t0(), restoring the v16.8.0 behavior where any DAI failure reliably
 *   triggers the AVIA fallback.
 */

package app.morphe.patches.paramount

import app.morphe.patcher.Fingerprint

// ---------------------------------------------------------------------------
// Patch 1a: VOD SSAI — createVodStreamRequest (3-arg)
// Unchanged from v16.8.0 — fingerprint still matches in v16.12.0.
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
// Unchanged from v16.8.0 — fingerprint still matches in v16.12.0.
//
// NOTE: v16.12.0's IMA SDK also adds a 5-arg overload taking a
// StreamRequest$StreamTrackingMode parameter, but it is not called anywhere
// in Paramount's app code (confirmed: the only real VOD call site,
// Lyk0;->run(), still calls the 3-arg overload). No fingerprint needed for it.
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
// Patch 1c: NEW (v16.12.0) — AdErrorEvent.AdErrorListener.onAdError()
//
// Anchored on the app's own literal "DAI Ad Error '" string, used when
// building the StringBuilder message passed into the Lml0 error wrapper.
// This string is app-authored (not part of the IMA SDK), so it survives
// IMA SDK updates and obfuscated-class renaming (the implementing class was
// Lcl0; in v16.12.0 but the name is not load-bearing for the fingerprint).
// ---------------------------------------------------------------------------

internal object OnAdErrorFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("DAI Ad Error '"),
    custom = { method, _ ->
        method.name == "onAdError" &&
            method.parameterTypes.size == 1 &&
            method.parameterTypes[0] ==
                "Lcom/google/ads/interactivemedia/v3/api/AdErrorEvent;"
    },
)

// ---------------------------------------------------------------------------
// Patch 2: Pause ads — CbsPauseWithAdsOverlay state machine
// Unchanged from v16.8.0.
// ---------------------------------------------------------------------------

internal object PauseAdOverlayFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("renderState: ", " not updating overlay."),
    custom = { method, _ ->
        method.definingClass.endsWith("CbsPauseWithAdsOverlay;")
    },
)
