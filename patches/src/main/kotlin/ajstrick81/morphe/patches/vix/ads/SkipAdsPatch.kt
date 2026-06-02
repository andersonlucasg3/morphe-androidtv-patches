package ajstrick81.morphe.patches.vix.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.vix.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Eliminates all ViX ad types: stubs the LuraPlayer FreeWheel and VAST/VMAP " +
        "configuration constructors so no ad URLs are ever requested, forces the skip policy to " +
        "its most permissive mode, and prevents the Innovid SSAI overlay from mounting.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — LuraFreewheelConfiguration.<init>
        //
        // Leaves the `enabled` boolean field at its JVM default (false).
        // The LuraPlayer ad scheduler reads this before fetching ad URLs —
        // with FreeWheel disabled the scheduler aborts the ad request early.
        // ─────────────────────────────────────────────────────────────────────
        LuraFreewheelConfigFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 2 — LuraAdsConfiguration.<init>
        //
        // Leaves all ad URL macros and break schedule fields null/empty.
        // Without a valid ad URL the VAST/VMAP request is never made and
        // no ad breaks are scheduled for any content session.
        // ─────────────────────────────────────────────────────────────────────
        LuraAdsConfigFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 3 — LuraAdsPolicySurrogate.<init>
        //
        // Leaves skipMode at its default enum value (most permissive).
        // Belt-and-suspenders: any ad that somehow survives Hooks 1–2 will
        // be immediately skippable with no countdown.
        // ─────────────────────────────────────────────────────────────────────
        LuraAdsPolicyFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 4 — InnovidHelper (start ad method)
        //
        // Prevents the Innovid SSAI WebView overlay from mounting.
        // The Innovid pipeline is entirely separate from LuraPlayer/FreeWheel —
        // Hooks 1–3 have no effect on it. Returning void here stops the
        // session before any network request or WebView instantiation.
        // ─────────────────────────────────────────────────────────────────────
        InnovidStartAdFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
    }
}
