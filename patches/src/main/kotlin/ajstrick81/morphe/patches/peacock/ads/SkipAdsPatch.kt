package ajstrick81.morphe.patches.peacock.ads

import ajstrick81.morphe.patches.peacock.shared.Constants
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Disables ad delivery via Sky SDK surgical targets (FreeWheel DI module " +
        "skip, MediaTailor SSAI layers), OkHttp interceptor, and WebView " +
        "shouldInterceptRequest wrapper. Validated v7.5.102.",
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
        // Replace NetworkingKt.getOkHttpClient() body entirely via a no-arg
        // static call to PeacockAdPatchHelper.buildOkHttpClient().
        //
        // History of VerifyErrors:
        //   v1.4.56 — offset 5, 4-instruction block passing v0 as Builder arg
        //             → type=Undefined at 0x16 (move-result-object after build())
        //   v1.4.57 — offset 5, single invoke-static {v0} passing Builder arg
        //             → type=Conflict at 0x10 (verifier ambiguous on v0 type
        //                at mid-method merge point)
        //
        // Fix — offset 0, no register arguments:
        //   At offset 0 no registers are live. invoke-static {} touches nothing.
        //   move-result-object v0 assigns a fresh OkHttpClient into an
        //   uninitialized register — the verifier always accepts this.
        //   return-object v0 exits cleanly. Original method body unreachable.
        GetOkHttpClientFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, Lajstrick81/morphe/extension/peacock/ads/PeacockAdPatchHelper;->buildOkHttpClient()Lokhttp3/OkHttpClient;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )

        // ── Layer 7 ─────────────────────────────────────────────────────────
        // WebView shouldInterceptRequest injection.
        //
        // PCAP/GREASE analysis confirmed ad segment delivery and FreeWheel
        // traffic travels through Chromium/WebView, bypassing OkHttp entirely.
        // XTVWebView's xtvClient does not override shouldInterceptRequest.
        //
        // Injection at instruction index 56 in XTVWebView.<init>(Context),
        // immediately before setWebViewClient(xtvClient). Wraps xtvClient via
        // PeacockWebViewHelper.wrapClient() which adds shouldInterceptRequest
        // with randomized responses to avoid FreeWheel fraud detection.
        XtvClientWrapFingerprint.method.addInstructions(
            56,
            """
                invoke-static {v1}, Lajstrick81/morphe/extension/peacock/ads/PeacockWebViewHelper;->wrapClient(Landroid/webkit/WebViewClient;)Landroid/webkit/WebViewClient;
                move-result-object v1
            """.trimIndent(),
        )

        // ── Layer 8 ─────────────────────────────────────────────────────────
        // Sky SDK FreeWheel DI module surgical removal.
        //
        // AddonInjectorImpl.di$lambda$0() is the Kodein DI wiring method that
        // imports all addon modules into the player container. The full module
        // import sequence is:
        //   coreAddonModule, coroutinesModule, contentProtectionModule,
        //   eventBoundaryModule, videoAdsConfigModule, mediaTailorModule,
        //   freewheelModule,  ← indices 16-17: iget-object + import$default
        //   networkApiModule, urlEncoder, platformAddonModule, lateBindingAddonModule
        //
        // Removing indices 16 and 17 prevents FreeWheel from ever being
        // registered in the DI container. The player has no FreeWheel addon:
        //   - No VMAP ad break schedule fetched
        //   - No VAST ad creative requested
        //   - No impression/quartile/completion pixels fired
        //   - No ad segments fetched or buffered
        //
        // This is the Sky SDK equivalent of Layer 4 (getSsaiConfigurationProvider
        // → null) but targeting CSAI/FreeWheel rather than SSAI/MediaTailor.
        // Works identically for VOD and live TV since the same DI wiring is
        // used for all content types — live TV simply never had a FreeWheel
        // module to begin with so removal is a no-op there.
        //
        // Note: removeInstruction is called twice on index 16 because after
        // the first removal index 17 shifts down to become index 16.
        FreewheelModuleSkipFingerprint.method.apply {
            removeInstruction(16) // iget-object v0, freewheelModule
            removeInstruction(16) // import$default(...) — now shifted to 16
        }
    }
}
