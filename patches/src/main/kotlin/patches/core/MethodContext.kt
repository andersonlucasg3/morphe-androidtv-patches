package patches.core

class MethodContext(private val methodName: String) {

    fun addInstruction(offset: Int, instruction: String) {
        println("Add instruction at offset $offset: $instruction")
    }

    fun replaceInstruction(offset: Int, instruction: String) {
        println("Replace instruction at offset $offset: $instruction")
    }
}
