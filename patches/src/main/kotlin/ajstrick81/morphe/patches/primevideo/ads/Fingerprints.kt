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

// ─────────────────────────────────────────────────────────────────────────────
// Tertiary target — media3 AdsMediaSource$ComponentListener.onAdPlaybackState()
// classes2.dex / smali/androidx/media3/exoplayer/source/ads/
//
// CSAI (client-side ad insertion) equivalent of the SSAI hooks above.
// AdsMediaSource handles dynamically loaded client-side ads via AdsLoader.
//
// The call chain:
//   AdsLoader.EventListener.onAdPlaybackState(AdPlaybackState)
//     → AdsMediaSource$ComponentListener.onAdPlaybackState()  ← OUR HOOK
//         → playerHandler.post(Runnable)
//             → AdsMediaSource.updateAdPlaybackState()
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
// Quaternary target — ExoPlayer2 AdsMediaSource$ComponentListener.onAdPlaybackState()
// classes4.dex / smali/com/google/android/exoplayer2/source/ads/
//
// ExoPlayer2 CSAI equivalent — identical structure to the media3 variant
// above but in the ExoPlayer2 namespace (Google Mobile Ads SDK / GMS Ads).
// Confirmed in classes4.dex with identical stopped flag check and Handler
// post pattern.
//
// Closes the final CSAI gap — together all four fingerprints above cover
// every ad delivery path in both the media3 and ExoPlayer2 pipelines.
// ─────────────────────────────────────────────────────────────────────────────
object AdsMediaSourceExo2ComponentListenerFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/exoplayer2/source/ads/AdsMediaSource\$ComponentListener;",
    name = "onAdPlaybackState",
    parameters = listOf("Lcom/google/android/exoplayer2/source/ads/AdPlaybackState;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC)
)

// ─────────────────────────────────────────────────────────────────────────────
// Quinary target — GoogleAdvertisingProperties.isAdvertisingOptOut()
// classes2.dex / smali/com/amazon/primevideo/advertising/
//
// Forces the advertising opt-out flag to true. The WASM runtime reads this
// via IgniteDevicePropertiesProvider under the property key
// "isAdvertisingOptOut" and uses it to determine whether to serve ads.
//
// Confirmed highly effective — reduced pre-roll from ~2 minutes to ~17
// seconds on Onn 4K TV. Operates at a higher level than the SSAI/CSAI
// hooks — potentially preventing the ad schedule from being built at all.
//
// The original method's exception handler already returns true when Google
// Play Services is unavailable. We make that the permanent state.
// ─────────────────────────────────────────────────────────────────────────────
object IsAdvertisingOptOutFingerprint : Fingerprint(
    definingClass = "Lcom/amazon/primevideo/advertising/GoogleAdvertisingProperties;",
    name = "isAdvertisingOptOut",
    parameters = listOf(),
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC)
)

// ─────────────────────────────────────────────────────────────────────────────
// Senary target — InterstitialAd.show()
// classes4.dex / smali/com/google/android/gms/ads/interstitial/
//
// Google Mobile Ads SDK interstitial ad display method. Returning void
// prevents full-screen interstitial ads from rendering even if they have
// already been loaded. This covers any interstitial-format ads (including
// the cart/purchase overlay style) delivered via the GMS Ads SDK rather
// than the WASM/Ignite pipeline.
//
// The abstract flag is intentional — this is an abstract method on the
// InterstitialAd class. Morphe resolves it against the concrete
// implementation at patch time.
// ─────────────────────────────────────────────────────────────────────────────
object InterstitialAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/interstitial/InterstitialAd;",
    name = "show",
    parameters = listOf("Landroid/app/Activity;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.ABSTRACT)
)
