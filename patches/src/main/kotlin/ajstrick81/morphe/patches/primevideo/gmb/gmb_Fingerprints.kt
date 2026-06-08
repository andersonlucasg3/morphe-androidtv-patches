package ajstrick81.morphe.patches.primevideo.gmb

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// Diagnostic target — GMBMessageProcessor.processMessage()
// smali/com/amazon/ignitionshared/GMBMessageProcessor.smali
//
// processMessage(String eventType, String payload) is called from native code
// (@CalledFromNative) every time the Ignite WASM runtime sends a message to
// the Java layer via the Generic Message Bus.
//
// Method signature:
//   public processMessage(String eventType, String payload) → int
//   p0 = this
//   p1 = eventType  ← the GMB event type string
//   p2 = payload    ← JSON payload for that event
//
// Amazon's internal logger already logs GMB messages but uses an internal
// pipeline invisible to ADB logcat. Our hook adds a standard
// android.util.Log.d() call so every event type appears under "GMB_DIAGNOSTIC"
//
// IMPORTANT: logGMBMessage is defined in the existing SkipAdsPatch.java
// extension class (primevideo/ads package) which is already confirmed to
// be correctly merged by Morphe. This avoids the NoClassDefFoundError
// that occurred when the method lived in a separate gmb package extension
// class that was not included in the merged DEX output.
// ─────────────────────────────────────────────────────────────────────────────
object GMBProcessMessageFingerprint : Fingerprint(
    definingClass = "Lcom/amazon/ignitionshared/GMBMessageProcessor;",
    name = "processMessage",
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/String;"
    ),
    returnType = "I",
    accessFlags = listOf(AccessFlags.PUBLIC)
)
