package ajstrick81.morphe.patches.tubi.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// Primary target — FoxImaAdListeners ad event dispatcher
// smali_classes5/com/fox/android/video/player/api/ima/listeners/
//
// This is the method that receives every Google IMA AdEvent from the
// StreamManager and dispatches it to FoxPlayer. The call chain is:
//
//   Google IMA SDK → AdEvent$AdEventListener.onAdEvent()
//     → adEventListener_delegate$lambda$10$lambda$9()  ← OUR HOOK
//       → FoxImaAdExtensionsKt.createFoxAdEvent()
//         → IFoxPlayer.dispatchAdEvent()
//             → FoxPlayer processes AD_BREAK_STARTED, AD_STARTED, etc.
//
// Returning void at index 0 means FoxPlayer never receives any ad event —
// no AD_BREAK_STARTED triggers playback lock, no AD_STARTED triggers
// ad rendering, no AD_POD_START initiates the ad break sequence.
//
// The IMA StreamManager still runs internally but its events are silently
// dropped before FoxPlayer can act on them. This is the Fox One equivalent
// of the Peacock HelioAdsLoader approach — intercept the custom ad event
// dispatcher before it reaches the player layer.
//
// Method signature (from smali):
//   private static final adEventListener_delegate$lambda$10$lambda$9(
//       FoxImaAdListeners, AdEvent) V
//
// Note: the $lambda$10$lambda$9 naming is Kotlin's nested lambda encoding.
// The method is private static final so Morphe resolves it unambiguously.
// ─────────────────────────────────────────────────────────────────────────────
object FoxImaAdEventListenerFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/api/ima/listeners/FoxImaAdListeners;",
    name = "adEventListener_delegate\$lambda\$10\$lambda\$9",
    parameters = listOf(
        "Lcom/fox/android/video/player/api/ima/listeners/FoxImaAdListeners;",
        "Lcom/google/ads/interactivemedia/v3/api/AdEvent;"
    ),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Secondary target — FoxPlayer.clearVodAds()
// smali_classes5/com/fox/android/video/player/FoxPlayer.smali
//
// FoxPlayer exposes a clearVodAds() method that clears the current VOD ad
// schedule. Calling this at the right moment (after content loads but before
// ad breaks fire) would remove pre-scheduled ad positions from FoxPlayer's
// internal timeline.
//
// Used as a secondary hook to cover any ad break data that was loaded before
// our FoxImaAdEventListenerFingerprint intercept fires. Together they provide:
//   - FoxImaAdEventListenerFingerprint: blocks all future ad events from IMA
//   - FoxPlayerClearVodAdsFingerprint: clears any pre-loaded ad schedule
//
// Strategy: hook clearVodAds() entry to also call our extension which
// additionally nulls out the IMA StreamManager reference in FoxImaAdListeners,
// ensuring no stale ad session can reactivate.
// ─────────────────────────────────────────────────────────────────────────────
object FoxPlayerClearVodAdsFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/FoxPlayer;",
    name = "clearVodAds",
    parameters = listOf(),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE)
)
