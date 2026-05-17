package patches.core

import java.io.File

object PatchLoader {

    fun loadPatchIds(): List<String> {
        val file = File("../patches-list.json")

        if (!file.exists()) {
            println("patches-list.json not found")
            return emptyList()
        }

        val content = file.readText()

        val regex = """"id"\s*:\s*"([^"]+)"""".toRegex()

        return regex.findAll(content)
            .map { it.groupValues[1] }
            .toList()
    }

    fun loadPatches(): List<Patch> {
        val ids = loadPatchIds()

        if (ids.isEmpty()) {
            println("No patches found in JSON")
        }

        return ids.mapNotNull { id ->
            val patch = PatchRegistry.getPatchById(id)
            if (patch == null) {
                println("Warning: No patch found for id '$id'")
            }
            patch
        }
    }
}
