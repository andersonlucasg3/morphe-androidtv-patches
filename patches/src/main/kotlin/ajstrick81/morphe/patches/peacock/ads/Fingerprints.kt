package ajstrick81.morphe.patches.peacock.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// Primary target — MultiPeriodAdsMediaSource$ComponentListener.a(AdPlaybackState)
// classes.dex / com/comcast/helio/hacks/multiperiodads/
//
// ComponentListener is the concrete implementation of MultiPeriodAdsLoader$
// EventListener. It is the single chokepoint where HelioAdsLoader pushes
// every AdPlaybackState update into the media3 ExoPlayer timeline. The full
// call chain is:
//
//   HelioAdsLoader (coroutines $1$1$1 and $1$1$1$1, plus direct method)
//     → HelioAdsLoader.h.a(AdPlaybackState)           ← interface dispatch
//       → ComponentListener.a(AdPlaybackState)         ← OUR HOOK
//         → Handler.post(Runnable_a(c=1, adState))
//           → Runnable_a.run()
//             → MultiPeriodAdsMediaSource.w = adState
//             → MultiPeriodAdsMediaSource.j0() / k0()  ← ExoPlayer timeline refresh
//
// Returning void at index 0 silences ALL AdPlaybackState deliveries from
// all three callers (direct, coroutine init, coroutine update lambda).
// MultiPeriodAdsMediaSource.w is never written and the ExoPlayer timeline
// never registers any ad periods.
//
// APK autopsy confirmed (v7.5.102, classes.dex):
//   - definingClass: Lcom/comcast/helio/hacks/multiperiodads/MultiPeriodAdsMediaSource$ComponentListener;
//   - parameter:     Landroidx/media3/common/AdPlaybackState;   (media3 common, NOT exoplayer2)
//   - ComponentListener lives only in classes.dex after patching; classes2.dex
//     does NOT contain it, so the patched version loads first at runtime.
//
// Prior builds used an incorrect class path (cvs/android/helio/ads) and wrong
// AdPlaybackState package (exoplayer2). Morphe fuzzy-matched anyway but a
// strict build will reject it. These are the verified correct values.
// ─────────────────────────────────────────────────────────────────────────────
object HelioAdPlaybackStateFingerprint : Fingerprint(
    definingClass = "Lcom/comcast/helio/hacks/multiperiodads/MultiPeriodAdsMediaSource\$ComponentListener;",
    name = "a",
    parameters = listOf("Landroidx/media3/common/AdPlaybackState;"),
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL)
)

// ─────────────────────────────────────────────────────────────────────────────
// Secondary target — HelioAdsLoader$1$1$1.invokeSuspend(Object)
// classes2.dex / com/comcast/helio/ads/insert/
//
// This is the Kotlin coroutine (suspend fun) that drives the complete ad
// schedule lifecycle for a playback session. Its invokeSuspend() method is
// the coroutine's state machine entry point, called both on initial launch
// and on each coroutine resume. The key operations inside are:
//
//   1. Fetches ad metadata from Helio's ad server
//   2. Constructs a new HelioAdPlaybackState (stores in HelioAdsLoader.f)
//   3. Dispatches via HelioAdsLoader.h.a(AdPlaybackState)
//      → ComponentListener.a() → Handler.post() → timeline update
//
// Returning COROUTINE_SUSPENDED immediately causes the coroutine framework
// to treat this suspension as permanent — the state machine never resumes,
// so steps 2 and 3 never execute. HelioAdsLoader.f stays null.
//
// All three dispatch callers guard with:
//   if-eqz f, :skip   ← HelioAdPlaybackState null check
//   if-eqz h, :skip   ← EventListener null check
//
// With f null (HelioAdPlaybackState never created), every dispatch path
// falls through the null guard as a no-op. This is the upstream backstop
// that complements the ComponentListener hook.
//
// Coroutine sentinel pattern:
//   sget-object p1, CoroutineSingletons->c   (c = COROUTINE_SUSPENDED, confirmed
//                                             via APK autopsy on v7.5.102)
//   return-object p1
//
// We reuse p1 (the incoming result Object) as the register since p-registers
// are always allocated and we exit before p1's original value is ever read.
// ─────────────────────────────────────────────────────────────────────────────
object HelioAdScheduleCoroutineFingerprint : Fingerprint(
    definingClass = "Lcom/comcast/helio/ads/insert/HelioAdsLoader\$1\$1\$1;",
    name = "invokeSuspend",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL)
)
