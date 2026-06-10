package ajstrick81.morphe.patches.primevideo.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.primevideo.shared.Constants

// ─────────────────────────────────────────────────────────────────────────────
// SUPPLEMENTARY PATCH — four additional hooks from classes2/classes4 analysis
//
// Hook 1: media3 AdsMediaSource$ComponentListener.onAdPlaybackState()
//   Closes the media3 CSAI gap.
//
// Hook 2: ExoPlayer2 AdsMediaSource$ComponentListener.onAdPlaybackState()
//   Closes the ExoPlayer2 CSAI gap (GMS Ads SDK).
//
// Hook 3: GoogleAdvertisingProperties.isAdvertisingOptOut()
//   Forces advertising opt-out to true — the single most impactful hook
//   in the patch. Confirmed reducing pre-roll from ~2 minutes to ~17
//   seconds. Tells the WASM runtime not to schedule ads for this device.
//
// Hook 4: InterstitialAd.show()
//   Prevents GMS Ads interstitial ads from rendering — covers any
//   full-screen or overlay format ads delivered via GMS Ads SDK.
//
// These four hooks complement the existing skipAdsPatch rather than
// replacing it. Both patches should be active simultaneously.
//
// Named supplementaryAdsPatch to avoid top-level val naming conflicts.
// ─────────────────────────────────────────────────────────────────────────────

@Suppress("unused")
val supplementaryAdsPatch = bytecodePatch(
    name = "Skip ads (supplementary)",
    description = "Adds CSAI suppression, advertising opt-out, and interstitial ad hooks to complement the primary Skip ads patch.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — media3 AdsMediaSource$ComponentListener.onAdPlaybackState()
        //
        // Closes the media3 CSAI delivery gap alongside the existing SSAI
        // hooks. Prevents client-side AdPlaybackState from being posted to
        // the player Handler.
        // ─────────────────────────────────────────────────────────────────────
        AdsMediaSourceComponentListenerFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 2 — ExoPlayer2 AdsMediaSource$ComponentListener.onAdPlaybackState()
        //
        // Same as Hook 1 but targeting the ExoPlayer2 variant in the GMS
        // Ads SDK (classes4.dex). Identical stopped flag + Handler post
        // pattern — return-void at index 0 gives unconditional suppression.
        // ─────────────────────────────────────────────────────────────────────
        AdsMediaSourceExo2ComponentListenerFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 3 — GoogleAdvertisingProperties.isAdvertisingOptOut()
        //
        // Forces advertising opt-out to true. Confirmed most impactful hook
        // in the entire patch — reduced pre-roll from ~2 minutes to ~17
        // seconds on Onn 4K TV.
        //
        // const/4 v0, 0x1 = true (opted out)
        // return v0
        // ─────────────────────────────────────────────────────────────────────
        IsAdvertisingOptOutFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 4 — InterstitialAd.show(Activity)
        //
        // Prevents Google Mobile Ads SDK interstitial ads from rendering.
        // Covers full-screen and overlay format ads delivered via GMS Ads
        // SDK rather than the WASM/Ignite pipeline — including any cart/
        // purchase overlay style ads that use the interstitial format.
        // ─────────────────────────────────────────────────────────────────────
        InterstitialAdShowFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
    }
}
