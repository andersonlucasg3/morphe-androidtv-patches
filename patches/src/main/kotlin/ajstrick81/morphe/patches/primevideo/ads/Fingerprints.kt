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
// classes3.dex / smali_classes3/com/google/android/exoplayer2/source/ads/
//
// The ExoPlayer2 SSAI source is bundled inside the Google Mobile Ads SDK
// (GMS Ads, 527 classes in classes3.dex). Same transformation applied.
// The ExoPlayer2 variant takes only the ImmutableMap — no Timeline parameter.
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

// ─────────────────────────────────────────────────────────────────────────────
// Tertiary target — AdsMediaSource$ComponentListener.onAdPlaybackState()
// classes2.dex / smali/androidx/media3/exoplayer/source/ads/
//
// This is the CSAI (client-side ad insertion) equivalent of the SSAI hooks
// above. While ServerSideAdInsertionMediaSource handles pre-scheduled
// server-stitched ads, AdsMediaSource handles dynamically loaded client-side
// ads via the AdsLoader interface.
//
// The call chain:
//   AdsLoader.EventListener.onAdPlaybackState(AdPlaybackState)
//     → AdsMediaSource$ComponentListener.onAdPlaybackState()  ← OUR HOOK
//         → playerHandler.post(Runnable)
//             → AdsMediaSource.updateAdPlaybackState()
//
// Returning void at index 0 prevents the CSAI AdPlaybackState from being
// posted to the player Handler — AdsMediaSource never sees the ad schedule.
//
// Together with the two SSAI fingerprints above this covers both CSAI and
// SSAI delivery paths in the media3 library.
// ─────────────────────────────────────────────────────────────────────────────
object AdsMediaSourceComponentListenerFingerprint : Fingerprint(
    definingClass = "Landroidx/media3/exoplayer/source/ads/AdsMediaSource\$ComponentListener;",
    name = "onAdPlaybackState",
    parameters = listOf("Landroidx/media3/common/AdPlaybackState;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC)
)

// ─────────────────────────────────────────────────────────────────────────────
// Quaternary target — GoogleAdvertisingProperties.isAdvertisingOptOut()
// classes2.dex / smali/com/amazon/primevideo/advertising/
//
// The advertising opt-out flag that Prime Video passes to the WASM runtime
// via IgniteDevicePropertiesProvider under the property key
// "isAdvertisingOptOut". The WASM runtime queries this at initialization
// and uses it to determine whether to serve ads to this device.
//
// Forcing this method to always return true (1 = opted out) signals to the
// WASM runtime that this device has opted out of advertising — potentially
// suppressing ad scheduling before ads are ever requested from the server.
//
// This operates at a higher level than the SSAI/CSAI hooks above:
//   - SSAI/CSAI hooks: suppress ads AFTER the schedule is built
//   - isAdvertisingOptOut hook: tells WASM NOT to build the schedule at all
//
// The original method's exception handler already returns true when Google
// Play Services is unavailable — Amazon designed this to fail-safe to
// opted-out. We make that the permanent state.
//
// No extension class needed — pure inline smali (const/4 v0, 0x1 / return v0).
// ─────────────────────────────────────────────────────────────────────────────
object IsAdvertisingOptOutFingerprint : Fingerprint(
    definingClass = "Lcom/amazon/primevideo/advertising/GoogleAdvertisingProperties;",
    name = "isAdvertisingOptOut",
    parameters = listOf(),
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC)
)
