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
        // IMPLEMENTATION: pure smali — no extension class required.
        //
        // Earlier attempts used a Java extension (SkipAdsPatch.shouldBlock) to
        // do the URL inspection, but the extension class never compiled into the
        // DEX due to a namespace mismatch in settings.gradle.kts.
        //
        // The v1.4.22 crash taught us the wrong lesson: we thought labels in
        // addInstructions caused the crash, but the actual cause was a missing
        // check-cast (returning java.lang.Object instead of WebResourceResponse).
        // Labels and conditional branches DO work in addInstructions.
        //
        // This implementation inlines the full ad-domain check directly in Dalvik
        // bytecode — no extension needed:
        //
        //   1. Get host from WebResourceRequest.getUrl().getHost()
        //   2. Check against each blocked domain using String.contains()
        //   3. If match → construct and return empty WebResourceResponse (blocked)
        //   4. If no match → fall through to original shouldInterceptRequest body
        //      which calls yo/b (LocalAssetsLoader) and returns null for external
        //      URLs — identical to pass-through behaviour
        //
        // Registers used: v0–v4 (within original .registers 10 budget)
        //   v0 = Uri / new WebResourceResponse
        //   v1 = host String / "text/plain"
        //   v2 = domain const / "utf-8"
        //   v3 = boolean result / empty byte[]
        //   v4 = ByteArrayInputStream
        //
        // Blocked domains (same set confirmed by AGP DNS test):
        //   dai.google.com, imasdk.googleapis.com, doubleclick.net,
        //   googletagmanager.com, googlesyndication.com
        // ─────────────────────────────────────────────────────────────────────
        TubiWebClientInterceptFingerprint.method.addInstructions(
            0,
            """
                if-eqz p2, :intercept_skip
                invoke-interface {p2}, Landroid/webkit/WebResourceRequest;->getUrl()Landroid/net/Uri;
                move-result-object v0
                if-eqz v0, :intercept_skip
                invoke-virtual {v0}, Landroid/net/Uri;->getHost()Ljava/lang/String;
                move-result-object v1
                if-eqz v1, :intercept_skip
                const-string v2, "dai.google.com"
                invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v3
                if-nez v3, :intercept_block
                const-string v2, "imasdk.googleapis.com"
                invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v3
                if-nez v3, :intercept_block
                const-string v2, "doubleclick.net"
                invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v3
                if-nez v3, :intercept_block
                const-string v2, "googletagmanager.com"
                invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v3
                if-nez v3, :intercept_block
                const-string v2, "googlesyndication.com"
                invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v3
                if-nez v3, :intercept_block
                goto :intercept_skip
                :intercept_block
                new-instance v0, Landroid/webkit/WebResourceResponse;
                const-string v1, "text/plain"
                const-string v2, "utf-8"
                const/4 v3, 0x0
                new-array v3, v3, [B
                new-instance v4, Ljava/io/ByteArrayInputStream;
                invoke-direct {v4, v3}, Ljava/io/ByteArrayInputStream;-><init>([B)V
                invoke-direct {v0, v1, v2, v4}, Landroid/webkit/WebResourceResponse;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/io/InputStream;)V
                return-object v0
                :intercept_skip
                nop
            """
        )
    }
}
