package patches.androidtv.paramount

import patches.core.Patch
import patches.core.PatchContext

object ParamountPatch : Patch() {

    override val name = "Paramount+ Hide Ads"
    override val description = "Disables IMA ad loading in Paramount+"

    override fun execute(context: PatchContext) {

        context.forClass("com.google.ads.interactivemedia.v3.api.ImaSdkFactory") {

            forMethod("zzb") {
                val result = ParamountFingerprint.find(this)

                if (result.found) {
                    val register = result.group(1)

                    addInstruction(
                        result.startOffset,
                        "const/4 $register, 0x0"
                    )

                    replaceInstruction(
                        result.startOffset + 2,
                        "const/4 $register, 0x0"
                    )
                }
            }
        }
    }
}
