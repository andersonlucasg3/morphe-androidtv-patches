package patches.core

class PatchContext {

    fun forClass(className: String, block: ClassContext.() -> Unit) {
        val classContext = ClassContext(className)
        classContext.block()
    }
}
