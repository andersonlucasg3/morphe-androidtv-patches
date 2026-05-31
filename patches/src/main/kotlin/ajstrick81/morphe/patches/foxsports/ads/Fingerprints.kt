package ajstrick81.morphe.patches.foxsports.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// Fox Sports Android TV — Ad suppression fingerprints
//
// Fox Corp ships the same compiled player SDK binary across Fox Sports,
// Fox One, and Tubi. All IMA/DAI and Yospace class paths are identical.
//
// Two ad pipelines:
//   VOD content   → Google IMA DAI  (Hooks 1–5)
//   Live content  → Yospace SSAI    (Hooks 6–8)
// ─────────────────────────────────────────────────────────────────────────────

object FoxImaAdEventListenerFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/api/ima/listeners/FoxImaAdListeners;",
    name = "adEventListener_delegate\$lambda\$10\$lambda\$9",
    strings = listOf("adEvent"),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL)
)

object FoxImaAdsLoadedListenerFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/api/ima/listeners/FoxImaAdListeners;",
    name = "adsLoadedListener_delegate\$lambda\$4\$lambda\$3",
    strings = listOf("onAdsManagerLoaded"),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL)
)

object FoxPlayerClearVodAdsFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/FoxPlayer;",
    name = "clearVodAds",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL)
)

object FoxImaVodStreamRequestFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/api/ima/loaders/FoxImaStreamIdLoader;",
    name = "requestVODDAIUrl",
    strings = listOf("requestVODDAIUrl() BEGIN..."),
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/Boolean;",
        "Lcom/fox/android/video/player/loaders/ImaStreamIdLoader\$ImaStreamUrlCallback;"
    ),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC)
)

object FoxImaLiveStreamRequestFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/api/ima/loaders/FoxImaStreamIdLoader;",
    name = "requestImaStreamId",
    strings = listOf("requestImaStreamId BEGIN..."),
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/Boolean;",
        "Lcom/fox/android/video/player/loaders/ImaStreamIdLoader\$ImaStreamIdCallback;"
    ),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC)
)

object YospaceDispatchAdEventFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/yospace/listener/YospaceAnalyticEventObserver;",
    name = "dispatchAdEvent",
    parameters = listOf("Lcom/fox/android/video/player/event/FoxAdEvent;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL)
)

object YospaceDispatchSlateEventFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/yospace/listener/YospaceAnalyticEventObserver;",
    name = "dispatchSlateEvent",
    parameters = listOf("Lcom/fox/android/video/player/event/FoxSlateEvent;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL)
)

object YospaceSeekPolicyFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/yospace/handler/YospaceSeekablePlaybackPolicyHandler;",
    name = "setHandleFastForwardSeek",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL)
)
