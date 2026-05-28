package ajstrick81.morphe.patches.tubi.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
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
        // This is the single most impactful change in the entire patch.
        // Every Google IMA ad event (AD_BREAK_STARTED, AD_STARTED, AD_POD_START,
        // AD_PROGRESS, AD_COMPLETED, AD_BREAK_ENDED, etc.) flows through this
        // one method before reaching FoxPlayer.
        //
        // returnEarly() inserts return-void at index 0. FoxPlayer.dispatchAdEvent()
        // is never called, so:
        //   - No AD_BREAK_STARTED → no playback lock / FF disable
        //   - No AD_STARTED → no ad rendering begins
        //   - No AD_POD_START → no ad pod sequence initiated
        //   - No AD_COMPLETED → no ad tracking beacons fired
        //   - No AD_BREAK_ENDED → player never exits ad break state
        //
        // The IMA StreamManager continues running internally in the Google IMA
        // SDK, meaning the DAI stream URL is still valid and content segments
        // continue to be served normally. Only the ad event dispatch is silenced.
        //
        // This is the Fox One / FoxOne equivalent of the same architecture —
        // Fox Corporation reuses FoxPlayer and FoxImaAdListeners across Tubi,
        // Fox One, Fox Sports, and other Fox properties. The same fingerprint
        // may apply with minimal changes to other Fox apps.
        // ─────────────────────────────────────────────────────────────────────
        FoxImaAdEventListenerFingerprint.method.returnEarly()

        // ─────────────────────────────────────────────────────────────────────
        // Hook 2 — FoxPlayer.clearVodAds()
        //
        // clearVodAds() is FoxPlayer's built-in method to clear the VOD ad
        // schedule. We hook its entry point to also call our extension which
        // nulls the IMA StreamManager reference in FoxImaAdListeners.
        //
        // This covers the edge case where ad break metadata was pre-loaded
        // into FoxPlayer's timeline before our Hook 1 fired. By clearing the
        // VOD ad schedule at this natural call site, we ensure no pre-scheduled
        // ad positions remain in FoxPlayer's internal state.
        //
        // Note: if clearVodAds() is not called during normal playback flow,
        // this hook is a no-op. It does not proactively trigger ad clearing —
        // it only amplifies the clearing when FoxPlayer already intends to do it.
        // ─────────────────────────────────────────────────────────────────────
        FoxPlayerClearVodAdsFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p0 }, Lajstrick81/morphe/extension/tubi/ads/SkipAdsPatch;->onClearVodAds(Ljava/lang/Object;)V
            """
        )
    }
}
