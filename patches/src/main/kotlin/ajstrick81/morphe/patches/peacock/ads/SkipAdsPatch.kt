package ajstrick81.morphe.patches.peacock.ads

import ajstrick81.morphe.patches.peacock.shared.Constants
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Disables ad delivery via five independent layers: MediaTailor SSAI proxy, " +
        "ObfuscatedProfileId SDK registry (Adobe, Comscore, Conviva, Freewheel, MParticle, " +
        "MediaTailor, Nielsen, OpenMeasurement), MediaTailor ad service constructor, " +
        "SSAI configuration provider (forces AdvertisingStrategy.None), and the Sky SDK " +
        "player engine ad break handler. Validated against v7.5.102.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        // ── Layer 1 ─────────────────────────────────────────────────────────
        // Kill MediaTailor SSAI proxy — empty string prevents proxy URL
        // configuration, disabling server-side ad insertion at the source.
        MediaTailorProxyHostFingerprint.method.addInstructions(
            0,
            """
                const-string v0, ""
                return-object v0
            """.trimIndent(),
        )

        // ── Layer 2 ─────────────────────────────────────────────────────────
        // Master kill switch — empty ObfuscatedProfileId array prevents all
        // 9 ad/analytics SDKs from being registered by the Sky SDK.
        ObfuscatedProfileIdValuesFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                new-array v0, v0, [Lcom/sky/core/player/addon/common/data/ObfuscatedProfileId;
                return-object v0
            """.trimIndent(),
        )

        // ── Layer 3 ─────────────────────────────────────────────────────────
        // Abort MediaTailor ad service construction — return null from the
        // factory method identified by its unique error string anchor.
        MediaTailorAdServiceMethodFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )

        // ── Layer 4 ─────────────────────────────────────────────────────────
        // Force AdvertisingStrategy.None — getSsaiConfigurationProvider()
        // returning null causes strategyForType() to take the confirmed
        // if-eqz → None branch for ALL playback types. No crash risk.
        SsaiConfigurationProviderFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )

        // ── Layer 5 ─────────────────────────────────────────────────────────
        // Kill ad breaks at the player engine level — return-void from
        // handleAdBreakStarted() so the Sky SDK player never enters an
        // ad break regardless of what upstream layers may have missed.
        HandleAdBreakStartedFingerprint.method.addInstructions(
            0,
            "return-void",
        )
    }
}
