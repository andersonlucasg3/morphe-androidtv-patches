package ajstrick81.morphe.patches.youtubetv.ads

import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.youtubetv.misc.contexthook.clientContextHookPatch
import ajstrick81.morphe.patches.youtubetv.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Suppresses video ads in YouTube Android TV by spoofing the OS name.",
) {
    dependsOn(clientContextHookPatch)
    compatibleWith(Constants.COMPATIBILITY)
    execute { }
}
