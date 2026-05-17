package patches.core

import patches.androidtv.paramount.ParamountPatch
import patches.androidtv.disney.DisneyPatch

object PatchRegistry {

    private val patches = listOf<Patch>(
        ParamountPatch,
        DisneyPatch
    )

    /**
     * Returns all registered patches
     */
    fun getPatches(): List<Patch> {
        return patches
    }

    /**
     * Find a patch by its ID
     */
    fun getPatchById(id: String): Patch? {
        return patches.find { it.id == id }
    }

    /**
     * Get all patch IDs
     */
    fun getPatchIds(): List<String> {
        return patches.map { it.id }
    }
}
