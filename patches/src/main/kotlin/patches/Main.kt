package patches

import patches.core.PatchContext
import patches.core.PatchRegistry

fun main() {

    println("Starting patch execution...\n")

    val context = PatchContext()

    val patches = PatchRegistry.getPatches()

    println("Loaded ${patches.size} patches\n")

    patches.forEach { patch ->
        println("Running patch: ${patch.name}")
        patch.execute(context)
        println()
    }

    println("Done.")
}
