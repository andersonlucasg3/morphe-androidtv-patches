package ajstrick81.morphe.patches.primevideo.gmb

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.primevideo.shared.Constants

// ─────────────────────────────────────────────────────────────────────────────
// DIAGNOSTIC PATCH — temporary, not for production use
//
// Intercepts GMBMessageProcessor.processMessage() and logs every GMB event
// type string directly to Android logcat using INLINE SMALI instructions
// that call android.util.Log.d() — a framework class always visible to
// any ClassLoader including the JNI native ClassLoader.
//
// WHY INLINE SMALI INSTEAD OF EXTENSION CLASS:
// The patched APK analysis confirmed that our extension DEX is never merged
// into the final APK — SkipAdsPatch.class is absent from the patched output.
// Any invoke-static pointing to our extension causes NoClassDefFoundError
// in the JNI ClassLoader context that calls processMessage().
//
// android.util.Log is part of the Android framework (framework.jar) and is
// always visible to all ClassLoaders including native JNI contexts.
// Calling it inline via smali requires zero external class references.
//
// Register layout in processMessage(.locals 3):
//   v0, v1, v2 = local registers (v0 currently used for log message string)
//   p0 = this (GMBMessageProcessor)
//   p1 = eventType (String) ← we log this
//   p2 = payload (String)   ← we log this
//
// We use v1 for the tag constant and v2 for a concat string, leaving v0
// untouched so the original method body continues working correctly.
//
// Usage after install:
//   adb logcat --pid=<PID> -v time | findstr "GMB_DIAGNOSTIC"
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
        // Inlines two android.util.Log.d() calls at index 0:
        //   1. Log "[TYPE] " + eventType   under tag "GMB_DIAGNOSTIC"
        //   2. Log "[PAYLOAD] " + payload  under tag "GMB_DIAGNOSTIC"
        //
        // Uses v1 and v2 as scratch registers. v0 is left alone since the
        // original method uses it immediately after our injected instructions.
        //
        // StringBuilder concat pattern:
        //   const-string v1, "prefix"
        //   invoke-virtual {v1, pX}, String.concat(String) → result in v1
        //   invoke-static {tag, v1}, Log.d(tag, msg) → int (discarded)
        // ─────────────────────────────────────────────────────────────────────
        GMBProcessMessageFingerprint.method.addInstructions(
            0,
            """
                const-string v1, "GMB_DIAGNOSTIC"
                const-string v2, "[TYPE] "
                invoke-virtual { v2, p1 }, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v2
                invoke-static { v1, v2 }, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                const-string v2, "[PAYLOAD] "
                invoke-virtual { v2, p2 }, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v2
                invoke-static { v1, v2 }, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
            """
        )
    }
}
