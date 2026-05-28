package ajstrick81.morphe.patches.foxsports.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.foxsports.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Suppresses Yospace SSAI ad events from reaching FoxPlayer and unlocks fast-forward during ad breaks.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — YospaceAnalyticEventObserver.dispatchAdEvent()
        //
        // Every Yospace ad event flows through this private method before
        // reaching FoxPlayer via a coroutine launch. Inserting return-void
        // at index 0 silences all ad events — the coroutine is never launched,
        // FoxPlayer.dispatchAdEvent() is never called, and no ad break state
        // is established in the player.
        //
        // The Yospace session itself continues normally, meaning the SSAI
        // stream URL and content segments are unaffected. Only the event
        // dispatch that triggers the player-level ad break is suppressed.
        // ─────────────────────────────────────────────────────────────────────
        YospaceDispatchAdEventFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 2 — YospaceAnalyticEventObserver.dispatchSlateEvent()
        //
        // Slate events are Fox's replacement content shown during live stream
        // ad breaks. Suppressing them alongside ad events ensures the player
        // never visually enters the ad/slate break state.
        // ─────────────────────────────────────────────────────────────────────
        YospaceDispatchSlateEventFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 3 — YospaceSeekablePlaybackPolicyHandler.setHandleFastForwardSeek()
        //
        // This setter installs the lambda that restricts FF seeking during
        // Yospace ad breaks. Returning void immediately discards the incoming
        // lambda without installing it, leaving handleFastForwardSeek at its
        // default (null / unrestricted) state.
        //
        // Effect: users can freely fast-forward through any ad break positions
        // on both VOD and live streams without seeing a restriction message.
        //
        // The handleRewindSeek equivalent is intentionally NOT patched — rewind
        // restriction during live ads is a safety mechanism to prevent users
        // from rewinding into ad content that has already been reported as viewed.
        // ─────────────────────────────────────────────────────────────────────
        YospaceSeekPolicySetFFFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
    }
}
