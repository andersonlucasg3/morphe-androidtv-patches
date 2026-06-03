package ajstrick81.morphe.patches.peacock.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ── Layer 1 ──────────────────────────────────────────────────────────────────
// Target: SSAIConfiguration$MediaTailor$AutomaticMediaTailor.getProxyHost()
// Returns the MediaTailor SSAI proxy URL. Returning "" disables SSAI.
// Confirmed present in v7.5.102.
internal object MediaTailorProxyHostFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/String;",
    custom = { method, classDef ->
        method.name == "getProxyHost" &&
            classDef.type.contains("AutomaticMediaTailor")
    },
)

// ── Layer 2 (REMOVED) ────────────────────────────────────────────────────────
// ObfuscatedProfileId.values() was confirmed in v6.11.212 but the class
// does not appear in the v7.5.102 DEX 2 scan — Sky SDK may have removed
// or restructured it. Re-add once class presence is confirmed in new version.

// ── Layer 3 ──────────────────────────────────────────────────────────────────
// Target: MediaTailorAdvertServiceFactoryImpl — method containing unique
// error string "Could not build MT Advertising service".
// Returning null aborts service construction.
// String confirmed present in v7.5.102 DEX 2.
internal object MediaTailorAdServiceMethodFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/Object;",
    strings = listOf("Could not build MT Advertising service"),
)

// ── Layer 4 ──────────────────────────────────────────────────────────────────
// Target: Configuration.getSsaiConfigurationProvider()
// Returning null forces Configuration$getDefaultAdvertisingStrategyProvider$1
// .strategyForType() to return AdvertisingStrategy.None for all playback
// types via confirmed if-eqz branch. AutomaticSSAI becomes unreachable.
internal object SsaiConfigurationProviderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lcom/sky/core/player/sdk/addon/SSAIConfigurationProvider;",
    custom = { method, classDef ->
        method.name == "getSsaiConfigurationProvider" &&
            classDef.type == "Lcom/sky/core/player/sdk/data/Configuration;"
    },
)

// ── Layer 5 ──────────────────────────────────────────────────────────────────
// Target: PlayerEngineItemImpl.handleAdBreakStarted(AdBreakStartedEvent)
// The Sky SDK player engine ad break entry point. Returning void immediately
// prevents the player from ever entering an ad break at the playback level.
// Confirmed present in v7.5.102 DEX 2 via androguard analysis.
internal object HandleAdBreakStartedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    custom = { method, classDef ->
        method.name == "handleAdBreakStarted" &&
            classDef.type ==
                "Lcom/sky/core/player/sdk/playerEngine/playerBase/PlayerEngineItemImpl;"
    },
)

// ── Extension Layer ───────────────────────────────────────────────────────────
// Target: androidx.media3.exoplayer.source.ads.ServerSideAdInsertionMediaSource
//         .setAdPlaybackStates(ImmutableMap, Timeline)
// Confirmed: Peacock v7.5.102 uses media3 (androidx.media3 in manifest).
internal object SetAdPlaybackStatesFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    custom = { method, classDef ->
        method.name == "setAdPlaybackStates" &&
            classDef.type.contains("ServerSideAdInsertionMediaSource")
    },
)
