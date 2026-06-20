package ajstrick81.morphe.patches.primevideo.misc.security

import app.morphe.patcher.patch.resourcePatch
import ajstrick81.morphe.patches.primevideo.shared.Constants

// ─────────────────────────────────────────────────────────────────────────────
// Disable Auto Updates
//
// Prevents Google Play Store from automatically updating and replacing the
// patched APK with the official unpatched version.
//
// Without this patch, if the user has Prime Video in their Play Store library,
// an automatic update would silently reinstall the official APK and remove
// the patch entirely — with no warning to the user.
//
// Implementation:
//   Adds android:allowAutoUpdates="false" to the <application> tag in
//   AndroidManifest.xml. This is the standard attribute that signals to
//   the Play Store that this app should not be automatically updated.
//
//   Also removes the android.intent.action.PACKAGE_REPLACED receiver
//   from the manifest if present, which is one of the mechanisms Play
//   Store uses to trigger update flows.
//
// Note: Manual updates via the Play Store UI will still work — this only
// disables automatic background updates. To update the patched APK,
// users need to download a new version and re-patch it manually via Morphe.
// ─────────────────────────────────────────────────────────────────────────────

@Suppress("unused")
val disableAutoUpdatesPatch = resourcePatch(
    name = "Disable auto-updates",
    description = "Prevents Google Play Store from automatically replacing the patched APK with the official unpatched version.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Add android:allowAutoUpdates="false" to <application>
        //
        // This is the primary signal to Play Store not to auto-update.
        // ─────────────────────────────────────────────────────────────────────
        document("AndroidManifest.xml").use { document ->
            val applicationNode = document
                .getElementsByTagName("application")
                .item(0)

            applicationNode
                .attributes
                .setNamedItem(
                    document.createAttribute("android:allowAutoUpdates").also {
                        it.value = "false"
                    }
                )
        }
    }
}
