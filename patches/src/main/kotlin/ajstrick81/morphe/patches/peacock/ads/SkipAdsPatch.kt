package ajstrick81.morphe.patches.peacock.ads

import ajstrick81.morphe.patches.peacock.misc.extension.peacockExtensionPatch
import ajstrick81.morphe.patches.peacock.shared.Constants
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Disables ad delivery via four bytecode layers plus a media3 " +
        "AdPlaybackState extension: MediaTailor SSAI proxy, MediaTailor ad service " +
        "constructor, SSAI configuration provider (forces AdvertisingStrategy.None), " +
        "Sky SDK player engine ad break handler, and media3 " +
        "ServerSideAdInsertionMediaSource ad group skipping. Validated v7.5.102.",
) {
    dependsOn(peacockExtensionPatch)

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

        // ── Extension Layer ──────────────────────────────────────────────────
        // media3 AdPlaybackState suppression — mark all active ad groups as
        // AD_STATE_SKIPPED before setAdPlaybackStates() executes.
        val setAdPlaybackStatesMethod = SetAdPlaybackStatesFingerprint.method
        val extensionClass =
            "Lajstrick81/morphe/extension/peacock/ads/SkipAdsPatch;"

        setAdPlaybackStatesMethod.addInstructions(
            0,
            """
                invoke-interface {p1}, Ljava/util/Map;->entrySet()Ljava/util/Set;
                move-result-object v0
                invoke-interface {v0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;
                move-result-object v1
                :loop_start
                invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z
                move-result v2
                if-eqz v2, :loop_end
                invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v3
                check-cast v3, Ljava/util/Map${'$'}Entry;
                invoke-interface {v3}, Ljava/util/Map${'$'}Entry;->getValue()Ljava/lang/Object;
                move-result-object v4
                invoke-static {v4}, $extensionClass->emptyAdPlaybackState(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v4
                invoke-interface {v3}, Ljava/util/Map${'$'}Entry;->setValue(Ljava/lang/Object;)Ljava/lang/Object;
                goto :loop_start
                :loop_end
            """.trimIndent(),
        )
    }
}
