package ajstrick81.morphe.patches.peacock.ads

import ajstrick81.morphe.patches.peacock.shared.Constants
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Disables ad delivery via three confirmed Sky SDK layers plus an OkHttp " +
        "interceptor that blocks ad CDN and analytics domains at the network layer, " +
        "replacing the AGH DNS dependency. Validated v7.5.102.",
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

        // ── Layer 6 ─────────────────────────────────────────────────────────
        // Inject AdBlockInterceptor into NetworkingKt.getOkHttpClient().
        // Inserts before offset 5 (after Builder.<init>, before the existing
        // OkHttpWorkaroundInterceptor new-instance) so both interceptors are
        // chained — AdBlockInterceptor runs first, then OkHttpWorkaroundInterceptor.
        // Suffix matching in AdBlockInterceptor covers future CDN group IDs
        // (g007+) without any patch update.
        GetOkHttpClientFingerprint.method.addInstructions(
            5,
            """
                new-instance v1, Lajstrick81/morphe/extension/peacock/ads/AdBlockInterceptor;
                invoke-direct {v1}, Lajstrick81/morphe/extension/peacock/ads/AdBlockInterceptor;-><init>()V
                invoke-virtual {v0, v1}, Lokhttp3/OkHttpClient${'$'}Builder;->addInterceptor(Lokhttp3/Interceptor;)Lokhttp3/OkHttpClient${'$'}Builder;
                move-result-object v0
            """.trimIndent(),
        )
    }
}
