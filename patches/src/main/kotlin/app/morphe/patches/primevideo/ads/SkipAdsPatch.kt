package ajstrick81.morphe.patches.primevideo.misc.extension

import app.morphe.patches.all.misc.extension.ExtensionHook
import app.morphe.patches.all.misc.extension.sharedExtensionPatch
import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// Fingerprint for the ATV launcher activity onCreate.
//
// The LEANBACK_LAUNCHER entry point is com.amazon.ignitionshared.MainActivity,
// declared via the IgniteActivity / IgnitionActivity activity-alias in the
// manifest. Using the full package segment /ignitionshared/MainActivity; to
// avoid any ambiguity with other MainActivity classes in the dex tree.
//
// targetBundleMethod = true targets onCreate(Landroid/os/Bundle;)V
// which is the standard Activity lifecycle entry point.
private object MainActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/amazon/ignitionshared/MainActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC)
)

// The sharedExtensionPatch and ExtensionHook APIs come from the official
// Morphe patches library (app.morphe.patches.all.misc.extension).
// This is the same infrastructure the official morphe-patches repo uses
// for YouTube, YouTube Music, etc. — no dependency on any third-party
// patches repo required.
val primeVideoExtensionPatch = sharedExtensionPatch(
    "primevideo",
    ExtensionHook(MainActivityOnCreateFingerprint)
)
