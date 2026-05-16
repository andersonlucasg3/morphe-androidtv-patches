object HideAdsPatch : Patch() {
    override val name = "Hide IMA Ads"
    override val description = "Neutralizes the IMA SDK configuration to skip ad loading."

    override fun execute(context: PatchContext) {
        // Search for the fingerprint in the ImaSdkFactory class
        context.forClass("com.google.ads.interactivemedia.v3.api.ImaSdkFactory") {
            // Find the specific method 'zzb' which contains the configuration logic
            forMethod("zzb") {
                val result = HideAdsFingerprint.find(it)
                
                if (result.found) {
                    val register = result.group(1) // Get the register name (e.g., p2)

                    // 1. Inject NULL before the call to break the URI input
                    it.addInstruction(
                        result.startOffset,
                        "const/4 $register, 0x0"
                    )

                    // 2. Inject NULL after the call to ensure the result is also NULL
                    // We target the offset right after the 'move-result-object'
                    it.replaceInstruction(
                        result.startOffset + 2, 
                        "const/4 $register, 0x0"
                    )
                }
            }
        }
    }
}