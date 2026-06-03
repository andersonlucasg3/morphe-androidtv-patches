package app.morphe.patches.peacocktvandroidtv.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ─────────────────────────────────────────────────────────
// LAYER 1: MediaTailor SSAI proxy host
// Target: SSAIConfiguration$MediaTailor$AutomaticMediaTailor.getProxyHost()
// Returning "" prevents proxy URL configuration → no SSAI.
// STATUS: Confirmed matching v6.11.212 and present in v7.5.102
// ─────────────────────────────────────────────────────────
internal object MediaTailorProxyHostFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/String;",
    custom = { method, classDef ->
        method.name == "getProxyHost" &&
            classDef.type.contains("AutomaticMediaTailor")
    },
)

// ─────────────────────────────────────────────────────────
// LAYER 2: ObfuscatedProfileId master kill switch
// Target: ObfuscatedProfileId.values()
// Returning empty array prevents all 9 ad/analytics SDKs from registering.
// STATUS: Confirmed matching v6.11.212 and present in v7.5.102
// ─────────────────────────────────────────────────────────
internal object ObfuscatedProfileIdValuesFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "[Lcom/sky/core/player/addon/common/data/ObfuscatedProfileId;",
    custom = { method, classDef ->
        method.name == "values" &&
            classDef.type == "Lcom/sky/core/player/addon/common/data/ObfuscatedProfileId;"
    },
)

// ─────────────────────────────────────────────────────────
// LAYER 3: MediaTailor ad service construction
// Target: MediaTailorAdvertServiceFactoryImpl — method containing unique
// error string "Could not build MT Advertising service".
// returnEarly(null) aborts service construction.
// Approach via RookieEnough/De-ReVanced — survives R8/D8 minification.
// STATUS: String confirmed present in v7.5.102 base.apk (DEX 2)
// ─────────────────────────────────────────────────────────
internal object MediaTailorAdServiceMethodFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/Object;",
    strings = listOf("Could not build MT Advertising service"),
)

// ─────────────────────────────────────────────────────────
// LAYER 4: SSAI Configuration Provider null override
// Target: Configuration.getSsaiConfigurationProvider()
// Returning null forces strategyForType() → AdvertisingStrategy.None
// for ALL playback types via confirmed if-eqz branch. No crash risk.
// STATUS: Class confirmed present in v7.5.102; named method, version-stable
// ─────────────────────────────────────────────────────────
internal object SsaiConfigurationProviderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lcom/sky/core/player/sdk/addon/SSAIConfigurationProvider;",
    custom = { method, classDef ->
        method.name == "getSsaiConfigurationProvider" &&
            classDef.type == "Lcom/sky/core/player/sdk/data/Configuration;"
    },
)

// ─────────────────────────────────────────────────────────
// LAYER 5: Ad break skip at playback level
// Target: PlayerEngineItemImpl.handleAdBreakStarted()
// This is the Sky SDK playback engine's ad break handler.
// In v7.5.102 confirmed as:
//   PlayerEngineItemImpl.handleAdBreakStarted(AdBreakStartedEvent)V
// The class also has skipAdvert(StitchedAdvert)V — we call it from
// handleAdBreakStarted to immediately skip every ad break.
// Anchor: inner class "PlayerEngineItemImpl$handleAdBreakStarted$1"
// is confirmed present in DEX 2 as a string literal.
// ─────────────────────────────────────────────────────────
internal object HandleAdBreakStartedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    custom = { method, classDef ->
        method.name == "handleAdBreakStarted" &&
            classDef.type ==
                "Lcom/sky/core/player/sdk/playerEngine/playerBase/PlayerEngineItemImpl;"
    },
)

// ─────────────────────────────────────────────────────────
// LAYER 5b: handleAdStartedEvent — fallback anchor
// Target: method in PlayerEngineItemImpl containing "handleAdStartedEvent"
// string. Used to locate the class if Layer 5 needs a string anchor.
// STATUS: String confirmed present in v7.5.102 DEX 2
// ─────────────────────────────────────────────────────────
internal object HandleAdStartedEventFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("handleAdStartedEvent"),
    custom = { method, classDef ->
        classDef.type ==
            "Lcom/sky/core/player/sdk/playerEngine/playerBase/PlayerEngineItemImpl;"
    },
)
