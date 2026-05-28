package ajstrick81.morphe.patches.tubi.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.tubi.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Prevents IMA DAI ad events from reaching FoxPlayer, suppressing all ad breaks.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — FoxImaAdListeners.adEventListener_delegate$lambda$10$lambda$9
        //
        // Every Google IMA ad event (AD_BREAK_STARTED, AD_STARTED, AD_POD_START,
        // AD_PROGRESS, AD_COMPLETED, AD_BREAK_ENDED, etc.) flows through this
        // one method before reaching FoxPlayer.dispatchAdEvent().
        //
        // Inserting return-void at index 0 means FoxPlayer never receives any
        // ad event — no AD_BREAK_STARTED triggers playback lock, no AD_STARTED
        // triggers ad rendering, no AD_POD_START initiates the ad break sequence.
        //
        // The IMA StreamManager continues running internally inside the Google
        // IMA SDK, meaning the DAI stream URL remains valid and content segments
        // are served normally. Only the ad event dispatch is silenced.
        //
        // This is the Fox One equivalent approach — Fox Corporation reuses
        // FoxPlayer and FoxImaAdListeners across Tubi, Fox One, Fox Sports, and
        // other Fox properties. The same fingerprint likely applies to all of them.
        //
        // Note: returnEarly() from app.morphe.util is not available in Morphe
        // 1.3.0. We insert return-void directly via addInstructions at index 0.
        // ─────────────────────────────────────────────────────────────────────
        FoxImaAdEventListenerFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 2 — FoxPlayer.clearVodAds()
        //
        // FoxPlayer's built-in clearVodAds() method clears the VOD ad schedule.
        // We hook its entry point to also call our extension which nulls the
        // IMA StreamManager reference in FoxImaAdListeners via reflection.
        //
        // This covers the edge case where ad break metadata was pre-loaded into
        // FoxPlayer's timeline before Hook 1 fired. Amplifying the clear at this
        // natural call site ensures no pre-scheduled ad positions remain.
        //
        // If clearVodAds() is not called during normal playback flow this hook
        // is a silent no-op — Hook 1 is the primary suppression mechanism.
        // ─────────────────────────────────────────────────────────────────────
        FoxPlayerClearVodAdsFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p0 }, Lajstrick81/morphe/extension/tubi/ads/SkipAdsPatch;->onClearVodAds(Ljava/lang/Object;)V
            """
        )
    }
}
