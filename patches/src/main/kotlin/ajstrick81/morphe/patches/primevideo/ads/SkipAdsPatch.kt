package ajstrick81.morphe.patches.primevideo.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.primevideo.misc.extension.primeVideoExtensionPatch
import ajstrick81.morphe.patches.primevideo.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Suppresses ad schedule and seeks past playing ads using the Player reference available in getStreamPositionUs.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(primeVideoExtensionPatch)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — media3 ServerSideAdInsertionMediaSource.setAdPlaybackStates()
        //
        // Strips all AdGroups from the incoming ImmutableMap before ExoPlayer
        // sees the ad schedule. Prevents new mid-roll breaks from being scheduled.
        // ─────────────────────────────────────────────────────────────────────
        SetAdPlaybackStatesMedia3Fingerprint.method.addInstructions(
            0,
            """
                invoke-static/range {p1 .. p1}, Lajstrick81/morphe/extension/primevideo/ads/SkipAdsPatch;->skipAllMedia3AdGroups(Lcom/google/common/collect/ImmutableMap;)Lcom/google/common/collect/ImmutableMap;
                move-result-object p1
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 2 — ExoPlayer2 ServerSideAdInsertionMediaSource.setAdPlaybackStates()
        //
        // Same as Hook 1 for the GMS Ads SDK ExoPlayer2 variant.
        // ─────────────────────────────────────────────────────────────────────
        SetAdPlaybackStatesExo2Fingerprint.method.addInstructions(
            0,
            """
                invoke-static/range {p1 .. p1}, Lajstrick81/morphe/extension/primevideo/ads/SkipAdsPatch;->skipAllExo2AdGroups(Lcom/google/common/collect/ImmutableMap;)Lcom/google/common/collect/ImmutableMap;
                move-result-object p1
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 3 — ServerSideAdInsertionUtil.getStreamPositionUs(Player, AdPlaybackState)
        //
        // This is the ATV equivalent of hoodles' mobile FSM state hook.
        // Called during active ad playback with live Player and AdPlaybackState
        // references — both required to calculate and execute the seek.
        //
        // Register layout (.locals 6):
        //   p0 = Player (interface) — live player reference
        //   p1 = AdPlaybackState   — contains ad group timing data
        //
        // The extension seekToAdBreakEnd() is called with both references.
        // It checks isPlayingAd() on the player — if true, calculates the
        // total remaining ad duration from the AdPlaybackState and seeks
        // the player forward past the ad break.
        //
        // If isPlayingAd() is false the method returns immediately (no-op)
        // and the original getStreamPositionUs continues normally.
        //
        // Standard invoke-static is safe here — p0 and p1 are the first
        // two parameters, well within the v0-v15 register range.
        // ─────────────────────────────────────────────────────────────────────
        GetStreamPositionUsFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0, p1}, Lajstrick81/morphe/extension/primevideo/ads/SkipAdsPatch;->seekToAdBreakEnd(Landroidx/media3/common/Player;Landroidx/media3/common/AdPlaybackState;)V
            """
        )
    }
}
