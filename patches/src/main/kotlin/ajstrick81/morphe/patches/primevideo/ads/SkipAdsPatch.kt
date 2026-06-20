package ajstrick81.morphe.patches.primevideo.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.primevideo.misc.extension.primeVideoExtensionPatch
import ajstrick81.morphe.patches.primevideo.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Multi-layer ad suppression targeting SSAI schedule, active ad playback, and impression reporting.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(primeVideoExtensionPatch)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — media3 ServerSideAdInsertionMediaSource.setAdPlaybackStates()
        //
        // Strips all AdGroups from the incoming SSAI ad schedule before
        // ExoPlayer sees it. Primary suppression for standard SSAI path.
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
        // Same strategy for the GMS Ads SDK ExoPlayer2 variant.
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
        // Hoodles-inspired seek hook. Fires during active ad playback with
        // live Player and AdPlaybackState references. Seeks past current ad
        // break duration when isPlayingAd() returns true.
        //
        // Covers PromoPlaybackExperience and any delivery path that bypasses
        // setAdPlaybackStates — operates at the playback layer regardless of
        // which WASM path initiated the ad.
        //
        // p0 = Player (live reference)
        // p1 = AdPlaybackState (current ad timing data)
        // ─────────────────────────────────────────────────────────────────────
        GetStreamPositionUsFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0, p1}, Lajstrick81/morphe/extension/primevideo/ads/SkipAdsPatch;->seekToAdBreakEnd(Landroidx/media3/common/Player;Landroidx/media3/common/AdPlaybackState;)V
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 4 — MetricsTransporter.transmit(SerializedBatch)
        //
        // Returns a fake SUCCESS UploadResult without making any network
        // request. Amazon's ad server receives no impression delivery data,
        // preventing it from accurately tracking ad viewing and reducing
        // scheduled ad load over time.
        //
        // Deception-over-brute-force: Amazon thinks impressions are being
        // reported normally — no retaliation triggered, no impression deficit
        // detection, no ad surge response.
        //
        // Inline smali constructs UploadResult("SUCCESS", "ok") directly:
        //   new-instance v0, UploadResult
        //   const-string v1, "SUCCESS"
        //   const-string v2, "ok"
        //   invoke-direct {v0, v1, v2}, UploadResult.<init>(String, String)
        //   return-object v0
        //
        // No extension class needed — UploadResult is in the app's own DEX
        // and is always accessible.
        // ─────────────────────────────────────────────────────────────────────
        MetricsTransporterTransmitFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Lcom/amazon/minerva/client/thirdparty/transport/UploadResult;
                const-string v1, "SUCCESS"
                const-string v2, "ok"
                invoke-direct {v0, v1, v2}, Lcom/amazon/minerva/client/thirdparty/transport/UploadResult;-><init>(Ljava/lang/String;Ljava/lang/String;)V
                return-object v0
            """
        )
    }
}
