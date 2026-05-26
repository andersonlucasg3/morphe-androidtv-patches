package ajstrick81.morphe.patches.primevideo.misc.extension

import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.primevideo.shared.Constants

// In Morphe 1.3.0 the extension system works differently from later versions.
// The app.morphe.patches.all.misc.extension namespace (sharedExtensionPatch,
// ExtensionHook) does not exist in 1.3.0.
//
// Extension classes compiled from extensions/src/main/java/ via the
// settings { extensions { } } block in settings.gradle.kts are automatically
// merged into the patched APK's DEX by the Morphe plugin at build time.
// No explicit MainActivity hook is required — the extension methods are
// called directly via invoke-static instructions inserted by SkipAdsPatch.
//
// This patch exists as a dependency anchor so SkipAdsPatch can declare
// dependsOn(primeVideoExtensionPatch), which signals to the Morphe plugin
// that the extension DEX must be merged before the patch is applied.
val primeVideoExtensionPatch = bytecodePatch(
    name = "Prime Video extension",
    description = "Integrates the Prime Video ATV extension for ad group skipping.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        // Intentionally empty — extension merging is handled automatically
        // by the Morphe 1.3.0 plugin via the settings.gradle.kts extensions block.
    }
}
