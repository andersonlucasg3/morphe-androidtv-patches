package ajstrick81.morphe.patches.primevideo.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.primevideo.misc.extension.primeVideoExtensionPatch
import ajstrick81.morphe.patches.primevideo.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Prevents server-side inserted ads from playing in the video stream.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(primeVideoExtensionPatch)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — media3 ServerSideAdInsertionMediaSource.setAdPlaybackStates()
        //
        // Register layout at entry of setAdPlaybackStates(ImmutableMap, Timeline):
        //   p0 = this (ServerSideAdInsertionMediaSource)
        //   p1 = ImmutableMap<Object, AdPlaybackState>   ← we transform this
        //   p2 = Timeline
        //
        // skipAllMedia3AdGroups() iterates every AdPlaybackState in the map
        // and calls withSkippedAdGroup(i) for each active group index,
        // returning a new ImmutableMap with all groups marked skipped before
        // ExoPlayer sees the map.
        //
        // withSkippedAdGroup() calls AdGroup.withAllAdsSkipped() which sets
        // AD_STATE_SKIPPED without touching isServerSideInserted, so the
        // SSAI validation in the original method continues to pass.
        //
        // invoke-static/range handles the high register number — p1 maps to
        // v17+ in this method due to its large local variable count.
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
        // Same strategy as Hook 1 but targeting the ExoPlayer2 variant inside
        // the bundled GMS Ads SDK. Structurally identical API — same
        // withSkippedAdGroup(int) contract, same removedAdGroupCount /
        // adGroupCount fields.
        // ─────────────────────────────────────────────────────────────────────
        SetAdPlaybackStatesExo2Fingerprint.method.addInstructions(
            0,
            """
                invoke-static/range {p1 .. p1}, Lajstrick81/morphe/extension/primevideo/ads/SkipAdsPatch;->skipAllExo2AdGroups(Lcom/google/common/collect/ImmutableMap;)Lcom/google/common/collect/ImmutableMap;
                move-result-object p1
            """
        )
    }
}
