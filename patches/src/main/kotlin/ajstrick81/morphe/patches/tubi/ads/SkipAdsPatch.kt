package ajstrick81.morphe.patches.tubi.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.tubi.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Eliminates all Tubi ad types: intercepts DAI stream requests before they reach dai.google.com to prevent pre-roll stitching, blocks IMA ad events from reaching FoxPlayer, prevents AdsManager from priming the cold-launch pre-roll, and suppresses the pause-screen image overlay ad.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — FoxImaAdListeners.adEventListener_delegate$lambda$10$lambda$9
        //
        // PRIMARY mid-roll/post-roll suppression. Every IMA ad event flows
        // through this single method before FoxPlayer.dispatchAdEvent().
        // return-void at index 0 silences all ad events for the session.
        // ─────────────────────────────────────────────────────────────────────
        FoxImaAdEventListenerFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 2 — FoxImaAdListeners.adsLoadedListener_delegate$lambda$4$lambda$3
        //
        // COLD LAUNCH PRE-ROLL backstop. Blocks BaseManager.init() from firing,
        // preventing the IMA timeline from being primed for the pre-roll.
        // Belt-and-suspenders alongside Hook 5 — if the DAI stream request
        // somehow succeeds, this ensures the stitched pre-roll is never armed.
        // ─────────────────────────────────────────────────────────────────────
        FoxImaAdsLoadedListenerFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 3 — FoxPlayer.clearVodAds()
        //
        // Belt-and-suspenders: calls our extension to null the IMA
        // StreamManager reference in FoxImaAdListeners via reflection,
        // ensuring no stale IMA session survives across content transitions.
        // ─────────────────────────────────────────────────────────────────────
        FoxPlayerClearVodAdsFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p0 }, Lajstrick81/morphe/extension/tubi/ads/SkipAdsPatch;->onClearVodAds(Ljava/lang/Object;)V
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 4 — ImagePauseAds.l(VideoApi, long)
        //
        // PAUSE AD suppression. Tubi's pause-screen image overlay is a
        // separate ad system from IMA — Hooks 1–3 have no effect on it.
        // return-void prevents the coroutine from fetching or displaying
        // the creative. Pause screen renders normally without the overlay.
        // ─────────────────────────────────────────────────────────────────────
        TubiPauseAdsFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 5 — FoxImaStreamIdLoader.requestVODDAIUrl(String, Boolean, ImaStreamUrlCallback)
        //
        // PRE-ROLL ROOT CAUSE suppression. Intercepts the DAI VOD stream
        // request before it reaches dai.google.com. Replicates a DNS-level
        // block in bytecode:
        //
        //   1. Call p3.onFailure("dai_blocked") → triggers Tubi's fallback
        //      path, which delivers a non-stitched direct content URL to
        //      FoxPlayer. Confirmed working via AGP DNS block test.
        //   2. return-void → DAI request never goes out.
        //
        // The onFailure() call is essential — omitting it leaves the callback
        // in a pending state and Tubi hangs waiting for a URL that never comes.
        //
        // p1 = adTagUrl (String)
        // p2 = forceRefresh (Boolean)
        // p3 = ImaStreamUrlCallback
        // ─────────────────────────────────────────────────────────────────────
        FoxImaVodStreamRequestFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "dai_blocked"
                invoke-interface {p3, v0}, Lcom/fox/android/video/player/loaders/ImaStreamIdLoader${"$"}ImaStreamUrlCallback;->onFailure(Ljava/lang/String;)V
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 6 — FoxImaStreamIdLoader.requestImaStreamId(String, Boolean, ImaStreamIdCallback)
        //
        // LIVE STREAM equivalent of Hook 5. Same intercept pattern for the
        // Pod/live stream path. Primarily a safety net — Tubi is VOD-only
        // so Hook 5 covers the common case.
        //
        // p3 = ImaStreamIdCallback
        // ─────────────────────────────────────────────────────────────────────
        FoxImaLiveStreamRequestFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "dai_blocked"
                invoke-interface {p3, v0}, Lcom/fox/android/video/player/loaders/ImaStreamIdLoader${"$"}ImaStreamIdCallback;->onFailure(Ljava/lang/String;)V
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 7 — xo/C$c.shouldInterceptRequest(WebView, WebResourceRequest)
        //
        // WEBVIEW PRE-ROLL ROOT CAUSE suppression. Full architectural detail
        // is in Fingerprints.kt. Short version: Tubi is a hybrid app, the SPA
        // drives the pre-roll entirely in JavaScript, and this is the only
        // native interception point that reaches the WebView network layer.
        //
        // IMPLEMENTATION: straight-line code only — no labels, no branches.
        //
        // Morphe's addInstructions does NOT support conditional branches or
        // labels in injected snippets. Any if-eqz/if-nez with a label target
        // produces bytecode that fails ART's DEX verifier at class load time,
        // crashing the app before launch (confirmed: v1.4.22 total failure).
        //
        // Fix exploits Android's shouldInterceptRequest() null contract:
        //   return non-null WebResourceResponse → intercept (block) request
        //   return null                         → pass through, Android handles normally
        //
        // TubiAdBlocker.shouldBlock() already returns null for non-ad domains,
        // so we return its result directly with zero branching required:
        //
        //   shouldBlock() → empty WebResourceResponse  = ad domain, request blocked
        //   shouldBlock() → null                       = not ad, Android handles normally
        //
        // The original yo/b (LocalAssetsLoader) is bypassed. yo/b only handles
        // local asset paths and returns null for all external URLs — functionally
        // identical to our null pass-through. The async coroutine xo/C$c$a
        // (page navigation analytics) is also skipped; not required for playback.
        // ─────────────────────────────────────────────────────────────────────
        TubiWebClientInterceptFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p2}, Lajstrick81/morphe/extension/tubi/ads/SkipAdsPatch;->shouldBlock(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v0
                return-object v0
            """
        )
    }
}
