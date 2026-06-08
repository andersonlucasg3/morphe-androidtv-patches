package ajstrick81.morphe.patches.primevideo.gmb

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.primevideo.shared.Constants

// ─────────────────────────────────────────────────────────────────────────────
// DIAGNOSTIC PATCH — temporary, not for production use
//
// Intercepts GMBMessageProcessor.processMessage() and logs every GMB event
// type string to Android logcat under the tag "GMB_DIAGNOSTIC".
//
// IMPORTANT: logGMBMessage lives in the existing SkipAdsPatch.java extension
// class at ajstrick81.morphe.extension.primevideo.ads.SkipAdsPatch — NOT in
// a separate gmb extension class. This is intentional — the ads extension
// class is already confirmed to be correctly merged by Morphe. A separate
// gmb extension class caused NoClassDefFoundError in JNI because the new
// package path was not included in the merged DEX output.
//
// Usage:
//   1. Commit this patch alongside the updated SkipAdsPatch.java
//   2. Apply the patched APK to the Onn device
//   3. Run: adb logcat --pid=<PID> -v time | findstr "GMB_DIAGNOSTIC"
//   4. Play content and observe all GMB event types during ad breaks
//   5. Note all ad/billing/overlay related event type strings
//   6. Remove this patch and build the production suppression patch
// ─────────────────────────────────────────────────────────────────────────────

@Suppress("unused")
val gmbDiagnosticPatch = bytecodePatch(
    name = "GMB Diagnostic",
    description = "Logs all GMB message event types to logcat for ad event discovery. Temporary diagnostic patch.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook — GMBMessageProcessor.processMessage(String eventType, String payload)
        //
        // Register layout:
        //   p0 = this (GMBMessageProcessor)
        //   p1 = eventType (String) ← log this
        //   p2 = payload (String)   ← log this
        //
        // Calls logGMBMessage in the existing SkipAdsPatch extension class
        // at index 0 — original processMessage continues normally after.
        //
        // Note: p1 and p2 are String parameters so they should be in the
        // accessible register range for a standard invoke-static call.
        // ─────────────────────────────────────────────────────────────────────
        GMBProcessMessageFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1, p2 }, Lajstrick81/morphe/extension/primevideo/ads/SkipAdsPatch;->logGMBMessage(Ljava/lang/String;Ljava/lang/String;)V
            """
        )
    }
}
