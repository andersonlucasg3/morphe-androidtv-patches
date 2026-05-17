package app.morphe.patches.paramount

import app.morphe.patcher.Patch
import app.morphe.patcher.PatchContext

object ParamountPatch : Patch(
    name = "Paramount+ Android TV",
    description = "Disables IMA ad loading in Paramount+"
) {

    override fun execute(context: PatchContext) {

        context.findMethod(
            className = "Lcom/google/ads/interactivemedia/v3/api/ImaSdkFactory;",
            methodName = "zzb"
        )?.let { method ->

            method.addInstruction(
                0,
                "const/4 p2, 0x0"
            )

            method.addInstruction(
                2,
                "const/4 p2, 0x0"
            )
        }
    }
}
