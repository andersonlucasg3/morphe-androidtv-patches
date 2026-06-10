package ajstrick81.morphe.patches.primevideo.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// Primary target — media3 SSAI ad schedule entry point
// classes.dex / smali/androidx/media3/exoplayer/source/ads/
//
// Called by the Ignite native layer (libignite.so + downloaded JS bundle)
// when it pushes the SSAI ad schedule into ExoPlayer. The ImmutableMap
// carries one AdPlaybackState per period UID, each containing the full set
// of AdGroups with their timing, duration, and individual ad URIs.
//
// Intercepting here — before the states are validated, posted to the playback
// Handler, and written into SharedMediaPeriod.adPlaybackState — is the
// earliest and cleanest point to nullify all ad groups.
// ─────────────────────────────────────────────────────────────────────────────
object SetAdPlaybackStatesMedia3Fingerprint : Fingerprint(
    definingClass = "Landroidx/media3/exoplayer/source/ads/ServerSideAdInsertionMediaSource;",
    name = "setAdPlaybackStates",
    parameters = listOf(
        "Lcom/google/common/collect/ImmutableMap;",
        "Landroidx/media3/common/Timeline;"
    ),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC)
)

// ─────────────────────────────────────────────────────────────────────────────
// Secondary target — ExoPlayer2 SSAI ad schedule entry point
// classes4.dex / smali/com/google/android/exoplayer2/source/ads/
//
// The ExoPlayer2 SSAI source is bundled inside the Google Mobile Ads SDK
// (GMS Ads). Same transformation applied. The ExoPlayer2 variant takes
// only the ImmutableMap — no Timeline parameter.
// ─────────────────────────────────────────────────────────────────────────────
object SetAdPlaybackStatesExo2Fingerprint : Fingerprint(
    definingClass = "Lcom/google/android/exoplayer2/source/ads/ServerSideAdInsertionMediaSource;",
    name = "setAdPlaybackStates",
    parameters = listOf(
        "Lcom/google/common/collect/ImmutableMap;"
    ),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC)
)
