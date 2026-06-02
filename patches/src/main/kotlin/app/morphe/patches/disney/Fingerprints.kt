/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/disneyplus/ads/Fingerprints.kt
 * ALL CREDIT GOES TO RookieEnough FOR THE ORIGINAL CODE
 *
 * Updated for Disney+ Android TV v26.8.0+rc6 (versionCode 1779314460)
 * - InsertionGetPointsFingerprint:  VALIDATED ✅  (class + method unchanged)
 * - InsertionGetRangesFingerprint:  VALIDATED ✅  (class + method unchanged)
 * - PauseAdSessionFingerprint:      NEW ✅  targets createPauseSession() in
 *                                   MediaXInterstitialController
 *
 * Pause ad fix v2:
 *   Previous target was onPauseScheduled() — this method already has a
 *   null guard on pauseSession and was not being matched reliably.
 *   Retargeted to createPauseSession(), which constructs and stores the
 *   MediaXPauseSession object. Returning null here means pauseSession is
 *   never populated, so onPauseScheduled()'s null guard fires and the
 *   entire pause ad lifecycle is silently skipped.
 */

package app.morphe.patches.disney

import app.morphe.patcher.Fingerprint

// ---------------------------------------------------------------------------
// Existing fingerprints — validated present and structurally unchanged in
// com.dss.sdk.internal.media.Insertion as of v26.8.0+rc6-2026.05.20
// ---------------------------------------------------------------------------

internal object InsertionGetPointsFingerprint : Fingerprint(
    returnType = "Ljava/util/List",
    custom = { method, _ ->
        method.name == "getPoints" &&
            method.definingClass == "Lcom/dss/sdk/internal/media/Insertion;"
    },
)

internal object InsertionGetRangesFingerprint : Fingerprint(
    returnType = "Ljava/util/List",
    custom = { method, _ ->
        method.name == "getRanges" &&
            method.definingClass == "Lcom/dss/sdk/internal/media/Insertion;"
    },
)

// ---------------------------------------------------------------------------
// Pause ad fingerprint — retargeted to createPauseSession()
//
// Target: MediaXInterstitialController.createPauseSession(MediaXPause)
//
// This method constructs the MediaXPauseSession and stores it in the
// controller's pauseSession field. The full pause ad lifecycle depends
// on this field being non-null:
//
//   createPauseSession(MediaXPause)              ← patch here
//     → new MediaXPauseSession(mediaXPause.into())
//     → iput pauseSession field
//     → return session
//
//   onPauseScheduled(MediaXPause)
//     → iget pauseSession
//     → if-eqz → return-void  (null guard — our kill switch)
//     → getPauseScheduled().onNext(session)
//       → subscriber renders pause ad overlay
//
// Patching strategy: return null (const/4 v0, 0x0 → return-object v0)
// at offset 0. pauseSession field is never populated, so onPauseScheduled's
// existing null guard fires and the render event is never published.
//
// Anchor string "mediaXPause" is a Kotlin non-null assertion label injected
// by the compiler from the source parameter name — stable across ProGuard
// minification since it is not subject to name obfuscation.
// ---------------------------------------------------------------------------

internal object PauseAdSessionFingerprint : Fingerprint(
    returnType = "Lcom/disneystreaming/nve/player/mel/MediaXPauseSession;",
    strings = listOf("mediaXPause"),
    custom = { method, _ ->
        method.name == "createPauseSession" &&
            method.definingClass ==
                "Lcom/disneystreaming/nve/player/mel/MediaXInterstitialController;"
    },
)
