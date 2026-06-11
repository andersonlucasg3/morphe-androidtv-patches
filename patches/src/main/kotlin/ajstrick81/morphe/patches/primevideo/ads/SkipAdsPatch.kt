package ajstrick81.morphe.patches.primevideo.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.primevideo.misc.extension.primeVideoExtensionPatch
import ajstrick81.morphe.patches.primevideo.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Prevents server-side inserted ads from playing by intercepting both the ad schedule and the segment delivery layer.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(primeVideoExtensionPatch)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — media3 ServerSideAdInsertionMediaSource.setAdPlaybackStates()
        //
        // Intercepts the SSAI ad schedule before ExoPlayer sees it.
        // Extension strips all AdGroups via withRemovedAdGroupCount().
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
        // Same strategy as Hook 1 for the ExoPlayer2 / GMS Ads SDK variant.
        // ─────────────────────────────────────────────────────────────────────
        SetAdPlaybackStatesExo2Fingerprint.method.addInstructions(
            0,
            """
                invoke-static/range {p1 .. p1}, Lajstrick81/morphe/extension/primevideo/ads/SkipAdsPatch;->skipAllExo2AdGroups(Lcom/google/common/collect/ImmutableMap;)Lcom/google/common/collect/ImmutableMap;
                move-result-object p1
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 3 — DefaultHttpDataSource.open(DataSpec)
        //
        // Intercepts every media segment fetch at the HTTP data source layer.
        // Reads the URI from the DataSpec, checks it against PCAP-confirmed
        // ad CDN domain patterns, and returns 0L (empty segment) for matches
        // before any network connection is established.
        //
        // This is the key hook for pre-roll suppression — it operates at the
        // segment delivery layer which fires BEFORE the WASM runtime pre-buffers
        // ad content, unlike setAdPlaybackStates which fires after buffering.
        //
        // Register layout at index 0 (.locals 14):
        //   p0 = this (DefaultHttpDataSource)
        //   p1 = DataSpec  ← uri:android.net.Uri is a public final field
        //   v0, v1 = scratch registers (safe to use before any original code)
        //
        // If URL matches ad pattern → return-wide 0x0 (empty, skip segment)
        // If URL is content → :cond_not_ad falls through to original method
        // ─────────────────────────────────────────────────────────────────────
        DefaultHttpDataSourceOpenFingerprint.method.addInstructions(
            0,
            """
                iget-object v0, p1, Landroidx/media3/datasource/DataSpec;->uri:Landroid/net/Uri;
                invoke-virtual {v0}, Landroid/net/Uri;->toString()Ljava/lang/String;
                move-result-object v0
                invoke-static {v0}, Lajstrick81/morphe/extension/primevideo/ads/SkipAdsPatch;->isAdSegmentUrl(Ljava/lang/String;)Z
                move-result v1
                if-eqz v1, :cond_not_ad
                const-wide/16 v0, 0x0
                return-wide v0
                :cond_not_ad
            """
        )
    }
}
