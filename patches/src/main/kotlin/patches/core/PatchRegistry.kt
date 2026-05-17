package patches.core

import patches.androidtv.paramount.ParamountPatch
import patches.androidtv.disney.DisneyPatch

object PatchRegistry {

    fun getPatches(): List<Patch> {
        return listOf(
            ParamountPatch,
            DisneyPatch
        )
    }
}
