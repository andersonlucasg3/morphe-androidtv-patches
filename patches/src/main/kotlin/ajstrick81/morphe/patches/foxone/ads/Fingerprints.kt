package ajstrick81.morphe.patches.foxone.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// Fox One v1.9.2 (com.fox.foxone) — Ad suppression fingerprints
//
// ARCHITECTURE NOTE:
// Fox One is a FULLY NATIVE app. Unlike Tubi (which uses a WebView-SPA hybrid
// where the pre-roll runs in a sandboxed Chrome renderer), Fox One uses the
// Fox player SDK directly with no WebView ad layer. This means all ad delivery
// is reachable via bytecode patches.
//
// Fox Corp ships the same compiled player SDK binary across Fox One, Tubi, and
// Fox Sports. The IMA/DAI classes (FoxImaAdListeners, FoxImaStreamIdLoader,
// FoxPlayer) are byte-for-byte identical across all three apps. The Yospace
// SSAI classes are Fox One / Fox Sports specific — Tubi is VOD-only and does
// not use Yospace.
//
// Two ad pipelines:
//   VOD content   → Google IMA DAI  (Hooks 1–3, 5–6)
//   Live content  → Yospace SSAI    (Hooks 7–8)
//
// No Hook 7 WebViewClient equivalent needed — Fox One has no WebView ad layer.
// No ImagePauseAds equivalent — Fox One has no pause ad overlay.
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// Hook 1 — FoxImaAdListeners.adEventListener_delegate$lambda$10$lambda$9
//
// Every Google IMA ad event (AD_BREAK_STARTED, AD_STARTED, AD_PROGRESS,
// AD_COMPLETED, etc.) flows through this single lambda before reaching
// FoxPlayer.dispatchAdEvent(). Silencing it means FoxPlayer never receives
// any ad lifecycle event — no ad break lock, no ad UI rendering, no ad pod
// sequencing. Primary suppression for all IMA-driven ad events.
//
// Unique string: "adEvent" (param null check, only occurrence in this class)
// ─────────────────────────────────────────────────────────────────────────────
object FoxImaAdEventListenerFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/api/ima/listeners/FoxImaAdListeners;",
    name = "adEventListener_delegate\$lambda\$10\$lambda\$9",
    strings = listOf("adEvent"),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Hook 2 — FoxImaAdListeners.adsLoadedListener_delegate$lambda$4$lambda$3
//
// Called when the IMA AdsManager finishes loading. This is the only place
// in the Fox IMA stack that calls imaStreamManager.init(), which primes the
// ad timeline and triggers the initial AD_BREAK_STARTED for the pre-roll.
// Blocking it prevents the IMA session from ever being initialised.
//
// Unique string: "onAdsManagerLoaded" (debug log, only occurrence in method)
// ─────────────────────────────────────────────────────────────────────────────
object FoxImaAdsLoadedListenerFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/api/ima/listeners/FoxImaAdListeners;",
    name = "adsLoadedListener_delegate\$lambda\$4\$lambda\$3",
    strings = listOf("onAdsManagerLoaded"),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Hook 3 — FoxPlayer.clearVodAds()
//
// Called during content transitions to clear VOD ad state. Hooks into the
// extension to null out any lingering IMA StreamManager reference via
// reflection, preventing stale IMA sessions from reactivating after clear.
// ─────────────────────────────────────────────────────────────────────────────
object FoxPlayerClearVodAdsFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/FoxPlayer;",
    name = "clearVodAds",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Hook 4 — FoxImaStreamIdLoader.requestVODDAIUrl(String, Boolean, ImaStreamUrlCallback)
//
// Initiates the Google DAI stream request for VOD content. The DAI-stitched
// stream embeds ad segments directly into the HLS manifest before the player
// ever sees the URL. This hook intercepts before the DAI request goes out:
// calls onFailure("dai_blocked") to trigger Fox One's fallback to a clean
// non-stitched stream, then returns void.
//
// Unique string: "requestVODDAIUrl() BEGIN..."
// ─────────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// Hook 5 — FoxImaStreamIdLoader.requestImaStreamId(String, Boolean, ImaStreamIdCallback)
//
// Live stream equivalent of Hook 4. Same intercept pattern — calls
// onFailure("dai_blocked") then returns void.
//
// Unique string: "requestImaStreamId BEGIN..."
// ─────────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// Hook 6 — YospaceAnalyticEventObserver.dispatchAdEvent(FoxAdEvent)
//
// LIVE CONTENT — Yospace SSAI ad event dispatcher. Every Yospace ad lifecycle
// event (ad start, ad progress, ad complete, ad break) flows through this
// coroutine launcher before reaching the player. Silencing it prevents
// FoxPlayer from entering ad-locked mode during live content, keeping the
// scrub bar active and preventing the ad UI from rendering.
//
// Fox One uses Yospace for all live sports/news content via
// foxdtc-video.akamaized.net with yo.* query parameters. This hook is
// the native-layer complement to the AGP Yospace parameter stripping rules.
//
// Unique class reference: YospaceAnalyticEventObserver$dispatchAdEvent$1
// ─────────────────────────────────────────────────────────────────────────────
object YospaceDispatchAdEventFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/yospace/listener/YospaceAnalyticEventObserver;",
    name = "dispatchAdEvent",
    parameters = listOf("Lcom/fox/android/video/player/event/FoxAdEvent;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Hook 7 — YospaceAnalyticEventObserver.dispatchSlateEvent(FoxSlateEvent)
//
// LIVE CONTENT — Yospace slate event dispatcher. Slate events are "black
// screen" ad break placeholders used during live content when no ad creative
// is available. Blocking alongside Hook 6 ensures both ad creatives and
// slate placeholders are suppressed during live playback.
//
// Unique class reference: YospaceAnalyticEventObserver$dispatchSlateEvent$1
// ─────────────────────────────────────────────────────────────────────────────
object YospaceDispatchSlateEventFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/yospace/listener/YospaceAnalyticEventObserver;",
    name = "dispatchSlateEvent",
    parameters = listOf("Lcom/fox/android/video/player/event/FoxSlateEvent;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Hook 8 — YospaceSeekablePlaybackPolicyHandler.setHandleFastForwardSeek
//
// LIVE CONTENT — Yospace enforces a seek policy that prevents fast-forwarding
// through ad breaks during live/DVR content. This hook nulls out the
// fast-forward seek handler so the player treats ad segments as freely
// seekable content. Without this, the player locks the scrub bar during
// Yospace ad breaks even when the ad event hooks prevent ad rendering.
//
// Unique string: "FF" (logged in handleFastForwardSeek$lambda$1)
// ─────────────────────────────────────────────────────────────────────────────
object YospaceSeekPolicyFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/yospace/handler/YospaceSeekablePlaybackPolicyHandler;",
    name = "setHandleFastForwardSeek",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL)
)
