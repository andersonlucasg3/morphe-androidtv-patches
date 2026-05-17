/*
 * Credit:
 * Original work by RookieEnough
 *
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/disneyplus/ads/Fingerprints.kt
 *
 * Modified for use in morphe-androidtv-patches
 */

package app.morphe.patches.disney

import app.morphe.patcher.Patch
import app.morphe.patcher.PatchContext

object DisneyPatch : Patch(
    name = "Disney+ Android TV",
    description = "Disables ad insertion in Disney+"
) {

    override fun execute(context: PatchContext) {

        context.findMethod(
            className = "Lcom/dss/sdk/internal/media/Insertion;",
            methodName = "getPoints"
        )?.let { method ->

            method.addInstruction(
                0,
                "return-object v0"
            )
        }

        context.findMethod(
            className = "Lcom/dss/sdk/internal/media/Insertion;",
            methodName = "getRanges"
        )?.let { method ->

            method.addInstruction(
                0,
                "return-object v0"
            )
        }
    }
}
