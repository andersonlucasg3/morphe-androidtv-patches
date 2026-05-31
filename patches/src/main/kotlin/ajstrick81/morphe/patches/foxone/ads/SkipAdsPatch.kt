package ajstrick81.morphe.patches.foxone.ads

import app.morphe.patcher.patch.bytecodePatch

val foxOneSkipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Suppresses all ad delivery systems in Fox One Android TV."
) {
    dependsOn(
        FoxImaAdEventListenerFingerprint,
        FoxImaAdsLoadedListenerFingerprint,
        FoxPlayerClearVodAdsFingerprint,
        FoxImaVodStreamRequestFingerprint,
        FoxImaLiveStreamRequestFingerprint,
        YospaceDispatchAdEventFingerprint,
        YospaceDispatchSlateEventFingerprint,
        YospaceSeekPolicyFingerprint
    )

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — FoxImaAdListeners.adEventListener_delegate$lambda$10$lambda$9
        //
        // Silences the IMA ad event lambda at index 0. Every ad lifecycle
        // event (AD_BREAK_STARTED, AD_STARTED, AD_PROGRESS, AD_COMPLETED)
        // flows through here. Returning void means FoxPlayer never receives
        // any ad event — no ad break lock, no ad UI, no ad pod sequencing.
        // Primary suppression for all VOD IMA-driven ads.
        // ─────────────────────────────────────────────────────────────────────
        FoxImaAdEventListenerFingerprint.method.addInstructions(
            0, "return-void"
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 2 — FoxImaAdListeners.adsLoadedListener_delegate$lambda$4$lambda$3
        //
        // Blocks IMA AdsManager initialisation. This is the only place that
        // calls imaStreamManager.init() to prime the ad timeline and trigger
        // the initial AD_BREAK_STARTED. Blocking it prevents the IMA session
        // from ever being confirmed — pre-roll suppression backstop.
        // ─────────────────────────────────────────────────────────────────────
        FoxImaAdsLoadedListenerFingerprint.method.addInstructions(
            0, "return-void"
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 3 — FoxPlayer.clearVodAds()
        //
        // Amplification hook that nulls out FoxPlayer's VOD ad state fields
        // at index 0, before the original clear logic runs. Ensures no stale
        // IMA session can reactivate ad delivery across content transitions.
        // ─────────────────────────────────────────────────────────────────────
        FoxPlayerClearVodAdsFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                iput-object v0, p0, Lcom/fox/android/video/player/FoxPlayer;->vodAds:Lcom/fox/android/video/player/args/StreamAds;
                iput-object v0, p0, Lcom/fox/android/video/player/FoxPlayer;->vodAdMarkers:[J
                iput-object v0, p0, Lcom/fox/android/video/player/FoxPlayer;->vodPlayedAdGroups:[Z
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 4 — FoxImaStreamIdLoader.requestVODDAIUrl(String, Boolean, ImaStreamUrlCallback)
        //
        // Intercepts the Google DAI stream request for VOD content before it
        // goes out. DAI-stitched streams embed ad segments directly into the
        // HLS manifest — Hooks 1–3 alone cannot remove segments already baked
        // into the stream. Calling onFailure("dai_blocked") triggers Fox One's
        // fallback path to serve a clean non-stitched stream instead.
        //
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
        // Hook 5 — FoxImaStreamIdLoader.requestImaStreamId(String, Boolean, ImaStreamIdCallback)
        //
        // Live stream equivalent of Hook 4. Same intercept pattern.
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
        // Hook 6 — YospaceAnalyticEventObserver.dispatchAdEvent(FoxAdEvent)
        //
        // LIVE CONTENT — Silences the Yospace ad event coroutine launcher.
        // Every Yospace ad lifecycle event (ad start, progress, complete,
        // break start/end) flows through this method before reaching FoxPlayer.
        // Returning void prevents FoxPlayer from entering ad-locked mode
        // during live sports/news content, keeping the scrub bar active and
        // preventing the ad UI from rendering.
        //
        // Fox One uses Yospace for all live content via
        // foxdtc-video.akamaized.net with yo.* HLS query parameters.
        // This hook is the native-layer complement to AGP Yospace param
        // stripping rules — together they suppress both the ad event pipeline
        // and the ad segment delivery.
        // ─────────────────────────────────────────────────────────────────────
        YospaceDispatchAdEventFingerprint.method.addInstructions(
            0, "return-void"
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 7 — YospaceAnalyticEventObserver.dispatchSlateEvent(FoxSlateEvent)
        //
        // LIVE CONTENT — Silences Yospace slate events. Slate events are
        // "black screen" placeholders shown during live ad breaks when no
        // creative is available. Blocking alongside Hook 6 ensures both ad
        // creatives and slate placeholders are fully suppressed.
        // ─────────────────────────────────────────────────────────────────────
        YospaceDispatchSlateEventFingerprint.method.addInstructions(
            0, "return-void"
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 8 — YospaceSeekablePlaybackPolicyHandler.setHandleFastForwardSeek
        //
        // LIVE CONTENT — Nulls out Yospace's fast-forward seek policy handler.
        // Without this, the player locks the scrub bar during Yospace ad breaks
        // even when the ad event hooks (6–7) prevent ad rendering — the seek
        // policy enforcement is a separate code path from the event dispatch.
        // Setting the handler to null makes the player treat ad segments as
        // freely seekable content throughout live/DVR playback.
        //
        // p1 = new handler value being set — we return before it's applied
        // ─────────────────────────────────────────────────────────────────────
        YospaceSeekPolicyFingerprint.method.addInstructions(
            0, "return-void"
        )
    }
}
