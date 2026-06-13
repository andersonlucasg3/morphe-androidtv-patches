package ajstrick81.morphe.patches.primevideo.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// Primary target — media3 SSAI ad schedule entry point
// classes.dex / smali/androidx/media3/exoplayer/source/ads/
//
// Called by the Ignite native layer when it pushes the SSAI ad schedule
// into ExoPlayer. Strips all AdGroups via withRemovedAdGroupCount().
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
// Tertiary target — ServerSideAdInsertionUtil.getStreamPositionUs(Player, AdPlaybackState)
// classes2.dex / smali/androidx/media3/exoplayer/source/ads/
//
// This is the ATV equivalent of hoodles' ServerInsertedAdBreakState.enter() hook
// from Prime Video Mobile. The parallel is exact:
//
//   Hoodles Mobile:              Our ATV:
//   enter(state, trigger, player) getStreamPositionUs(player, adPlaybackState)
//   trigger.getBreak().duration   adPlaybackState.getAdGroup(i).durationsUs
//   player.seekTo(adBreakEnd)     player.seekTo(adBreakEndMs)
//   doTrigger(NO_MORE_ADS)        withRemovedAdGroupCount() already handles this
//
// This method is called by media3 while isPlayingAd() == true to calculate
// the current stream position relative to the ad timeline. At this point:
//   p0 = Player (live reference — NOT a WeakRef, no capture needed)
//   p1 = AdPlaybackState (contains ad group times and durations)
//
// When isPlayingAd() is true we have everything needed to:
//   1. Get current ad group index from the player
//   2. Sum the remaining ad durations in that group from AdPlaybackState
//   3. Seek the player to currentPosition + totalRemainingAdDuration
//
// This fires at the segment playback layer — AFTER the WASM pre-buffers ads
// but DURING playback, meaning it can skip even pre-buffered content.
// Combined with setAdPlaybackStates (prevents new ad groups) this gives
// defense in depth across both the scheduling and playback layers.
//
// Parameters confirmed from smali:
//   p0 = Player (interface: seekTo(J)V, isPlayingAd()Z,
//                getCurrentAdGroupIndex()I, getCurrentPosition()J)
//   p1 = AdPlaybackState (getAdGroup(I) -> AdGroup with durationsUs:[J, count:I)
// ─────────────────────────────────────────────────────────────────────────────
object GetStreamPositionUsFingerprint : Fingerprint(
    definingClass = "Landroidx/media3/exoplayer/source/ads/ServerSideAdInsertionUtil;",
    name = "getStreamPositionUs",
    parameters = listOf(
        "Landroidx/media3/common/Player;",
        "Landroidx/media3/common/AdPlaybackState;"
    ),
    returnType = "J",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)
)
