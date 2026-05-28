package ajstrick81.morphe.patches.foxsports.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// Primary target — YospaceAnalyticEventObserver.dispatchAdEvent()
// smali_classes4/com/fox/android/video/player/yospace/listener/
//
// This is the Yospace SSAI equivalent of FoxImaAdListeners.adEventListener
// in Tubi. Every Yospace ad event (AD_BREAK_STARTED, AD_STARTED, AD_PROGRESS,
// AD_COMPLETED, AD_BREAK_ENDED, etc.) flows through this private method before
// reaching FoxPlayer.dispatchAdEvent() via the coroutine lambda.
//
// The call chain is:
//   Yospace SDK → YospaceAnalyticEventObserver.onAnalyticUpdate()
//     → dispatchAdEvent(FoxAdEvent)           ← OUR HOOK
//       → coroutine launch { dispatchAdEvent$1.invokeSuspend() }
//         → EventPlayer.dispatchAdEvent(FoxAdEvent)
//             → FoxPlayer processes ad break events
//
// Returning void at index 0 silences all Yospace ad events before FoxPlayer
// sees them — no ad break start, no ad rendering, no seek lock activated.
//
// The Yospace StreamManager continues to initialize and report playhead
// positions to Yospace's server, meaning SSAI session management remains
// intact. Only the ad event dispatch to FoxPlayer is suppressed.
//
// This is the same pattern as Tubi's FoxImaAdEventListenerFingerprint —
// Fox Corporation reuses YospaceAnalyticEventObserver across Fox Sports,
// Fox One, and other properties using Yospace SSAI.
// ─────────────────────────────────────────────────────────────────────────────
object YospaceDispatchAdEventFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/yospace/listener/YospaceAnalyticEventObserver;",
    name = "dispatchAdEvent",
    parameters = listOf("Lcom/fox/android/video/player/event/FoxAdEvent;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Secondary target — YospaceAnalyticEventObserver.dispatchSlateEvent()
// smali_classes4/com/fox/android/video/player/yospace/listener/
//
// Slate events are Fox's term for replacement content shown during ad breaks
// on live streams (e.g. "We'll be right back" slates). Suppressing slate
// events alongside ad events ensures the player never enters the visual
// ad/slate break state.
//
// Same pattern as dispatchAdEvent — return void at index 0.
// ─────────────────────────────────────────────────────────────────────────────
object YospaceDispatchSlateEventFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/yospace/listener/YospaceAnalyticEventObserver;",
    name = "dispatchSlateEvent",
    parameters = listOf("Lcom/fox/android/video/player/event/FoxSlateEvent;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Tertiary target — YospaceSeekablePlaybackPolicyHandler.setHandleFastForwardSeek()
// smali_classes4/com/fox/android/video/player/yospace/handler/
//
// YospaceSeekablePlaybackPolicyHandler is a singleton that manages seek
// behavior during Yospace SSAI ad breaks. The static field handleFastForwardSeek
// holds a Function3 lambda that restricts or allows FF seeks based on ad
// break position.
//
// setHandleFastForwardSeek(Function3) is called during player initialization
// to install the FF restriction. By hooking this setter and discarding the
// incoming lambda (setting null instead), the FF restriction is never installed
// and users can seek freely through ad break positions on live streams.
//
// This is the Fox Sports equivalent of the "unable to fast forward" lock
// seen in Prime Video ATV — intercepted at the Java installation point
// rather than the WASM/GMB layer.
// ─────────────────────────────────────────────────────────────────────────────
object YospaceSeekPolicySetFFFingerprint : Fingerprint(
    definingClass = "Lcom/fox/android/video/player/yospace/handler/YospaceSeekablePlaybackPolicyHandler;",
    name = "setHandleFastForwardSeek",
    parameters = listOf("Lkotlin/jvm/functions/Function3;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL)
)
