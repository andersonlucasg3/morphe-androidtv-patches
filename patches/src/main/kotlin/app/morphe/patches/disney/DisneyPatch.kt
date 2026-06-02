/*
 * Credit:
 * Original work by RookieEnough aka The G.O.A.T :)
 *
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/disneyplus/ads/Fingerprints.kt
 *
 * Modified for use in morphe-androidtv-patches
 *
 * Validated against Disney+ Android TV v26.8.0+rc6-2026.05.20
 * Package:     com.disney.disneyplus
 * VersionCode: 1779314460
 */

package app.morphe.patches.disney

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disneyPatch = bytecodePatch(
    name = "Disney+ Android TV",
    description = "Removes mid-roll / pre-roll ads and pause ads.",
) {
    compatibleWith(AppCompatibilities.DISNEY_PLUS_TV)

    execute {
        // ------------------------------------------------------------------
        // Patch 1 & 2: Pre-roll / mid-roll SGAI/SSAI ad insertion
        //
        // Insertion.getPoints() returns the list of InsertionPoints (ad cue
        // positions) for a media item. Returning an empty list causes the
        // player to see zero ad cues and skip all break scheduling.
        //
        // Insertion.getRanges() returns ad range windows used by:
        //   - Media3ExtensionsKt.allowedLiveInterstitials()  (live ad gating)
        //   - InsertionJsonAdapters                          (serialisation)
        // Emptying it prevents live interstitial gating from admitting any
        // ad range and stops serialisation from writing range data.
        //
        // Both methods are simple iget-object / return-object pairs, so
        // prepending a fresh ArrayList return at offset 0 is safe — the
        // original iget is never reached and the field is never read.
        // ------------------------------------------------------------------
        arrayOf(
            InsertionGetPointsFingerprint,
            InsertionGetRangesFingerprint,
        ).forEach { fingerprint ->
            fingerprint.method.addInstructions(
                0,
                """
                    new-instance v0, Ljava/util/ArrayList;
                    invoke-direct { v0 }, Ljava/util/ArrayList;-><init>()V
                    return-object v0
                """.trimIndent(),
            )
        }

        // ------------------------------------------------------------------
        // Patch 3: Pause ads
        //
        // MediaXInterstitialController.createPauseSession(MediaXPause) is
        // the method that constructs the MediaXPauseSession and stores it
        // in the controller's pauseSession field.
        //
        // The downstream method onPauseScheduled() already contains a null
        // guard on that field:
        //
        //   iget-object v2, v1, ...->pauseSession
        //   if-eqz v2, :return_void    ← fires if pauseSession is null
        //   getPauseScheduled().onNext(v2)
        //     → subscriber renders pause ad overlay
        //
        // Patching strategy: return null at offset 0 of createPauseSession().
        // pauseSession is never populated, onPauseScheduled()'s existing null
        // guard fires on every pause event, and the overlay is never rendered.
        //
        // Using the framework's existing null guard as our kill switch means
        // no risk of NPE anywhere else in the controller — clear() and other
        // lifecycle methods all perform their own null checks on pauseSession.
        // ------------------------------------------------------------------
        PauseAdSessionFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
    }
}
