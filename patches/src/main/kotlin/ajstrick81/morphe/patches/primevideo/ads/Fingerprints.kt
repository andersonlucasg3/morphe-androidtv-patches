package ajstrick81.morphe.patches.primevideo.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// Primary target — media3 SSAI ad schedule entry point
// classes.dex / smali/androidx/media3/exoplayer/source/ads/
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
// Hoodles-inspired seek hook. Called during active ad playback with live
// Player and AdPlaybackState references. When isPlayingAd() is true, seeks
// the player past the current ad break duration.
//
// Covers the PromoPlaybackExperience path and any other ad delivery mechanism
// that bypasses setAdPlaybackStates — operates during active playback
// regardless of which delivery path initiated the ad.
//
// p0 = Player (interface: seekTo(J)V, isPlayingAd()Z,
//              getCurrentAdGroupIndex()I, getCurrentPosition()J)
// p1 = AdPlaybackState (getAdGroup(I) -> AdGroup with durationsUs:[J)
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

// ─────────────────────────────────────────────────────────────────────────────
// Quaternary target — MetricsTransporter.transmit(SerializedBatch)
// classes2.dex / smali/com/amazon/minerva/client/thirdparty/transport/
//
// The Java-layer impression reporting pipeline that successfully uploads
// ad metrics to Amazon's servers (HTTP 200 OK, 224 uploads in a single
// ad session). Confirmed in logcat: "Successfully uploaded metrics; code: 200"
//
// Returning a fake SUCCESS UploadResult prevents Amazon from receiving
// impression delivery reports — without impression data, Amazon's ad server
// cannot accurately track whether ads are being viewed, which should reduce
// ad scheduling pressure over time (the impression deficit effect).
//
// Strategy: construct a fake UploadResult(SUCCESS, "ok") and return it
// without making any network request. The caller sees a successful upload
// and moves on normally.
//
// UploadResult constructor: <init>(String uploadStatus, String uploadMessage)
// SUCCESS constant: UploadResult.SUCCESS = "SUCCESS"
//
// This is the deception-over-brute-force approach applied to the metrics
// layer — Amazon's ad server thinks impressions are being reported normally.
// ─────────────────────────────────────────────────────────────────────────────
object MetricsTransporterTransmitFingerprint : Fingerprint(
    definingClass = "Lcom/amazon/minerva/client/thirdparty/transport/MetricsTransporter;",
    name = "transmit",
    parameters = listOf(
        "Lcom/amazon/minerva/client/thirdparty/transport/SerializedBatch;"
    ),
    returnType = "Lcom/amazon/minerva/client/thirdparty/transport/UploadResult;",
    accessFlags = listOf(AccessFlags.PUBLIC)
)
