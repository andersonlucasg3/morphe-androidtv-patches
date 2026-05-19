/*
 * Credit:
 * Original work by RookieEnough aka The G.O.A.T :)
 *
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/disneyplus/ads/Fingerprints.kt
 *
 * Modified for use in morphe-androidtv-patches
 */

package app.morphe.patches.disney

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.fingerprint

@Suppress("unused")
val disneyPatch = bytecodePatch(
    name = "Disney+ Android TV",
    description = "Disables ad insertion in Disney+",
) {
    compatibleWith(AppCompatibilities.DISNEY_PLUS_TV)

    val insertionGetPointsFingerprint = fingerprint {
        custom { method, _ ->
            method.definingClass == "Lcom/dss/sdk/internal/media/Insertion;" &&
                method.name == "getPoints"
        }
    }

    val insertionGetRangesFingerprint = fingerprint {
        custom { method, _ ->
            method.definingClass == "Lcom/dss/sdk/internal/media/Insertion;" &&
                method.name == "getRanges"
        }
    }

    execute {
        insertionGetPointsFingerprint.method.addInstructions(
            0,
            "return-object v0",
        )

        insertionGetRangesFingerprint.method.addInstructions(
            0,
            "return-object v0",
        )
    }
}
