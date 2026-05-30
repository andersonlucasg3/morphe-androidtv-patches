package ajstrick81.morphe.patches.tubi.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// Hook 1 — FoxImaAdListeners.adEventListener_delegate$lambda$10$lambda$9
// classes.dex / com/fox/android/video/player/api/ima/listeners/
//
// Every Google IMA AdEvent (AD_BREAK_STARTED, AD_STARTED, AD_POD_START,
// AD_PROGRESS, AD_COMPLETED, AD_BREAK_ENDED, etc.) flows through this single
// private static method before reaching FoxPlayer.dispatchAdEvent().
//
// Returning void at index 0 means FoxPlayer never receives any ad event —
// no AD_BREAK_STARTED triggers playback lock, no AD_STARTED triggers ad
// rendering, no AD_POD_START initiates the ad break sequence.
//
// The IMA StreamManager continues running internally; the DAI stream URL
// remains valid and content segments are served normally. Only the event
// dispatch to FoxPlayer is silenced.
//
// Fox Corporation reuses FoxPlayer and FoxImaAdListeners across Tubi,
// Fox One, Fox Sports, and other Fox properties. The same fingerprint
// applies to all of them.
//
// PRIMARY suppression mechanism — blocks mid-roll and post-roll ad events.
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
// Hook 2 — FoxImaAdListeners.adsLoadedListener_delegate$lambda$4$lambda$3
// classes.dex / com/fox/android/video/player/api/ima/listeners/
//
// Called by the Google IMA SDK when the AdsManager finishes loading. This
// method is the ONLY place in the Fox IMA stack that calls
// BaseManager.init(), which is what tells the IMA SDK to prime the ad
// timeline and fire the initial AD_BREAK_STARTED for the pre-roll.
//
// Call chain:
//   IMA SDK → AdsLoader.AdsLoadedListener.onAdsManagerLoaded()
//     → adsLoadedListener_delegate$lambda$4$lambda$3()     ← OUR HOOK
//         stores StreamManager in imaStreamManager
//         registers adEventListener / adErrorListener
//         calls BaseManager.init()                         ← triggers pre-roll
//         calls ImaStreamIdCallback.onSuccess(streamId)    ← session confirm
//
// Returning void at index 0 prevents init() from firing, so the IMA SDK
// never primes the pre-roll position and AD_BREAK_STARTED is never issued.
//
// Stream playback URL is NOT affected — it arrives via the separate
// ImaStreamIdLoader.requestVODDAIUrl() → ImaStreamUrlCallback.onSuccess()
// path, which runs independently before this listener ever fires.
// Blocking this lambda only suppresses the IMA session confirmation and
// the pre-roll trigger; content loads and plays normally.
//
// COLD LAUNCH PRE-ROLL suppression — complements Hook 1 which covers
// mid-rolls. Together the two hooks eliminate all IMA ad delivery.
// ─────────────────────────────────────────────────────────────────────────────
object FoxImaAdsLoadedListenerFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/api/ima/listeners/FoxImaAdListeners;",
    name = "adsLoadedListener_delegate\$lambda\$4\$lambda\$3",
    parameters = listOf(
        "Lcom/fox/android/video/player/api/ima/listeners/FoxImaAdListeners;",
        "Lcom/google/ads/interactivemedia/v3/api/AdsManagerLoadedEvent;"
    ),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Hook 3 — FoxPlayer.clearVodAds()
// classes.dex / com/fox/android/video/player/FoxPlayer
//
// FoxPlayer's built-in method that clears the VOD ad schedule. Hooked to
// also call the Java extension which nulls the IMA StreamManager reference
// in FoxImaAdListeners via reflection, preventing any stale IMA session
// from reactivating ad delivery after a clearVodAds call.
//
// Belt-and-suspenders cleanup for edge cases where the StreamManager
// reference survives past Hook 2. Silent no-op if clearVodAds() is not
// called during the playback lifecycle.
// ─────────────────────────────────────────────────────────────────────────────
object FoxPlayerClearVodAdsFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/FoxPlayer;",
    name = "clearVodAds",
    parameters = listOf(),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE)
)

// ─────────────────────────────────────────────────────────────────────────────
// Hook 4 — ImagePauseAds.l(VideoApi, long)
// classes9.dex / com/tubitv/features/player/presenters/pauseads/
//
// Tubi-specific pause ad entry point. Called when the user pauses playback,
// passing the current VideoApi metadata and an elapsed time offset. The
// method launches a coroutine (ImagePauseAds$f) that:
//   1. Fetches a PauseAdsResponse from Tubi's ad server
//   2. Downloads the creative image
//   3. Displays it via PauseAdsView as an overlay on the pause screen
//   4. Fires impression/view tracking events on show and dismiss
//
// This is entirely separate from the IMA DAI linear ad pipeline — it is
// a Tubi-owned display ad system with its own network requests and view
// hierarchy. Hooks 1–3 have no effect on it.
//
// Returning void at index 0 prevents the coroutine from ever being created.
// The pause screen renders normally; the ad overlay is simply never fetched
// or displayed. No tracking events fire (no impression to report).
// ─────────────────────────────────────────────────────────────────────────────
object TubiPauseAdsFingerprint : Fingerprint(
    definingClass = "Lcom/tubitv/features/player/presenters/pauseads/ImagePauseAds;",
    name = "l",
    parameters = listOf("Lcom/tubitv/core/api/models/VideoApi;", "J"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Hook 5 — FoxImaStreamIdLoader.requestVODDAIUrl(String, Boolean, ImaStreamUrlCallback)
// classes7.dex / com/fox/android/video/player/api/ima/loaders/
//
// This is the method that initiates the Google DAI stream request for VOD
// content. When called, it constructs a StreamRequest via
// ImaSdkFactory.createVodStreamRequest(), sets ad tag parameters, registers
// the AdsLoadedListener and AdErrorListener, then calls
// AdsLoader.requestStream() — which reaches out to dai.google.com to obtain
// a DAI-stitched HLS stream URL with the pre-roll physically embedded at
// position 0.
//
// The stitched stream is what survives Hooks 1–4. Because the pre-roll video
// segment is baked into the HLS manifest before FoxPlayer ever sees the URL,
// no IMA event hook can remove it — the player simply plays what's there.
//
// DNS-level blocking of dai.google.com (confirmed via AGP test) eliminates
// the pre-roll because:
//   - The DAI stream request fails
//   - ImaStreamUrlCallback.onFailure() is triggered
//   - Tubi falls back to a non-stitched direct content URL
//   - Content plays clean with no embedded pre-roll
//
// This hook replicates that DNS block in bytecode:
//   - Call p3.onFailure("dai_blocked") to trigger Tubi's fallback path
//   - return-void to prevent the DAI request from ever being made
//
// The onFailure() call is essential — without it, the callback is never
// invoked and Tubi hangs waiting for a stream URL that never arrives.
//
// p1 = adTagUrl (String)
// p2 = forceRefresh (Boolean)
// p3 = ImaStreamUrlCallback (callback to deliver stream URL to FoxPlayer)
//
// No extension needed — pure smali with const-string + invoke-interface.
// ─────────────────────────────────────────────────────────────────────────────
object FoxImaVodStreamRequestFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/api/ima/loaders/FoxImaStreamIdLoader;",
    name = "requestVODDAIUrl",
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/Boolean;",
        "Lcom/fox/android/video/player/loaders/ImaStreamIdLoader\$ImaStreamUrlCallback;"
    ),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC)
)

// ─────────────────────────────────────────────────────────────────────────────
// Hook 6 — FoxImaStreamIdLoader.requestImaStreamId(String, Boolean, ImaStreamIdCallback)
// classes7.dex / com/fox/android/video/player/api/ima/loaders/
//
// Live stream equivalent of Hook 5. Uses createPodStreamRequest() instead of
// createVodStreamRequest() but follows the same pattern — constructs a
// StreamRequest and calls AdsLoader.requestStream() to obtain a DAI-stitched
// live stream. Included for completeness; Tubi is primarily VOD so Hook 5
// covers the common case.
//
// Same intercept pattern: call p3.onFailure("dai_blocked") then return-void.
// p3 = ImaStreamIdCallback
// ─────────────────────────────────────────────────────────────────────────────
object FoxImaLiveStreamRequestFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/api/ima/loaders/FoxImaStreamIdLoader;",
    name = "requestImaStreamId",
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/Boolean;",
        "Lcom/fox/android/video/player/loaders/ImaStreamIdLoader\$ImaStreamIdCallback;"
    ),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC)
)

// ─────────────────────────────────────────────────────────────────────────────
// Hook 7 — xo/C$c.shouldInterceptRequest(WebView, WebResourceRequest)
// classes11.dex / xo/ (R8-minified TvWebFragment$TubiWebClient)
//
// This is the most significant architectural discovery of this session.
// Tubi on Android TV is a HYBRID app — it loads a full React/JavaScript SPA
// at https://ott-androidtv.tubitv.com inside a TubiWebView (which wraps
// wendu/dsbridge DWebView). The SPA handles the ENTIRE pre-roll ad lifecycle
// in JavaScript:
//
//   Chrome WebView renderer (sandboxed process)
//     → SPA requests ad creative from dai.google.com
//     → SPA plays pre-roll video as a JS <video> element
//     → SPA tracks impression via its own JS ad engine
//     → SPA calls JS→Native bridge: startNativePlayer({use_tubi_native_player:true})
//         → xo/C.startNativePlayer() launches ExoPlayer with content URL
//
// This is why Hooks 1–6 (which all patch Java classes) have ZERO effect on
// the pre-roll: the ad plays before ExoPlayer even initialises. The logcat
// confirmed this — ExoPlayerImpl.Init fires ~2 minutes after app launch,
// long after the 39-second pre-roll session has ended.
//
// This is also the root cause behind unpatched pre-rolls in Fox Sports and
// Fox One from previous sessions. Fox Corp ships the same WebView-SPA hybrid
// architecture across all their TV apps. DNS rules worked because they block
// at the network layer which affects the Chrome renderer process. Native
// bytecode patches could not reach the JS ad engine.
//
// xo/C$c is the TubiWebClient (TvWebFragment$TubiWebClient after R8 mapping).
// It extends android.webkit.WebViewClient and overrides shouldInterceptRequest(),
// which Android calls for EVERY resource the WebView tries to load — including
// the ad creative requests, ad tracking pixels, and DAI stream requests.
//
// Our hook:
//   1. Before the existing yo/b (LocalAssetsLoader) check runs, call our
//      Java extension TubiAdBlocker.shouldBlock(WebResourceRequest)
//   2. If the request URL matches an ad domain pattern, return an empty
//      WebResourceResponse (HTTP 200, empty body) — the ad request silently
//      fails from the SPA's perspective
//   3. If not an ad request, fall through to the existing yo/b logic
//
// This is the bytecode equivalent of the AGP DNS block — implemented as a
// WebViewClient interceptor rather than a network-layer filter, which means
// it works with or without AGH/AGP on the network.
//
// Extension required: TubiAdBlocker.java — needs to construct
// WebResourceResponse objects and check URL host patterns. Pure smali
// cannot construct WebResourceResponse; Java interop is necessary.
//
// Blocked domains (matching confirmed via AGP DNS test):
//   dai.google.com         — Google DAI stream stitching (pre-roll source)
//   imasdk.googleapis.com  — IMA SDK JS loader
//   googletagmanager.com   — Ad tag manager
//   doubleclick.net        — Ad delivery
//   googlesyndication.com  — Ad syndication
// ─────────────────────────────────────────────────────────────────────────────
object TubiWebClientInterceptFingerprint : Fingerprint(
    definingClass = "Lxo/C\$c;",
    name = "shouldInterceptRequest",
    parameters = listOf(
        "Landroid/webkit/WebView;",
        "Landroid/webkit/WebResourceRequest;"
    ),
    returnType = "Landroid/webkit/WebResourceResponse;",
    accessFlags = listOf(AccessFlags.PUBLIC)
)
