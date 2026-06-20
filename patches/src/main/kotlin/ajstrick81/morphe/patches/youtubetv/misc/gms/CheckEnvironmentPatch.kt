package ajstrick81.morphe.patches.youtubetv.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.youtubetv.shared.Constants

/**
 * Injects the GMS core availability check into the YouTube TV main activity's
 * onCreate method. This is the bridge from smali to the runtime extension
 * class GmsCoreSupportPatch.java.
 *
 * Ported from Morphe mobile: youtube/misc/check/CheckEnvironmentPatch.kt
 *
 * TV-SPECIFIC ADAPTATIONS:
 * - Battery optimization check REMOVED (TVs don't aggressively kill processes,
 *   and TV Settings UI is unsuitable for dialog-based interaction)
 * - Dialog-based UX REPLACED with Toast-based UX (dialogs are hard to navigate
 *   with a TV remote control)
 */
@Suppress("unused")
val checkEnvironmentPatch = bytecodePatch(
    name = "Check environment",
    description = "Injects GMS core availability check into YouTube TV main activity.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    // TODO Phase 0: Find the main activity class in the YouTube TV APK.
    // In mobile YouTube: Lcom/google/android/apps/youtube/app/watchwhile/MainActivity;
    // For YouTube TV: likely Lcom/google/android/apps/youtube/tv/... or similar.
    // Use the MainActivityFingerprint or ApplicationFingerprint to locate the
    // correct onCreate method.
    //
    // The injection should be:
    //   invoke-static/range { p0 .. p0 },
    //       Lajstrick81/morphe/extension/youtubetv/patches/GmsCoreSupportPatch;->checkGmsCore(Landroid/app/Activity;)V
    //
    // This must be injected near the beginning of onCreate(), after super.onCreate()
    // but before any GMS-dependent initialization.

    execute {
        // Placeholder — implementation requires Phase 0 RE data.
        // The fingerprint(s) for main activity onCreate will be added to
        // misc/gms/Fingerprints.kt after APK decompilation.

        // Example injection (will be activated after Phase 0):
        // MainActivityOnCreateFingerprint.method.addInstructions(
        //     firstInstructionAfterSuperOnCreate,
        //     """
        //         invoke-static/range { p0 .. p0 },
        //             Lajstrick81/morphe/extension/youtubetv/patches/GmsCoreSupportPatch;->checkGmsCore(Landroid/app/Activity;)V
        //     """
        // )
    }
}
