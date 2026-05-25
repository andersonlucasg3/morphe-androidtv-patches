package app.morphe.patches.hbomax.ads

import app.morphe.patcher.fingerprint.MethodFingerprint
import app.morphe.patcher.fingerprint.MethodFingerprintResult
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patcher.fingerprint.AccessFlags

// ─────────────────────────────────────────────────────────────────────────────
// BoltNonLinearAdsRequest — Nonlinear (overlay) ad request serializer
// Located in classes4.dex
// Serializer descriptor string survives R8 because kotlinx.serialization
// requires it at runtime. Both getAdRequestType and getPlaybackId are
// patched to return empty strings to avoid NPE on @NotNull callers.
// write$Self is patched to suppress advertisingInfo (field index 2) from
// the JSON body and zero out playbackId (field index 5).
// ─────────────────────────────────────────────────────────────────────────────

object BoltNonLinearAdsRequestGetAdRequestTypeFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("BoltNonLinearAdsRequest(adRequestType="),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;" &&
            methodDef.name == "getAdRequestType"
    }
)

object BoltNonLinearAdsRequestGetPlaybackIdFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;" &&
            methodDef.name == "getPlaybackId"
    }
)

object BoltNonLinearAdsRequestWriteSelfFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC or AccessFlags.FINAL,
    parameters = listOf(
        "Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;",
        "Lbr/d;",
        "Lar/f;"
    ),
    strings = listOf("com.wbd.adtech.bolt.BoltNonLinearAdsRequest"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;" &&
            methodDef.name == "write\$Self"
    }
)

// ─────────────────────────────────────────────────────────────────────────────
// BoltDynamicAdFetcher — Nonlinear ad fetch coroutine continuation
// Located in classes4.dex
// The inner class $fetchNonLinearAds$1 holds the coroutine continuation.
// After fetchNonLinearAds returns, move-result-object v8 is followed by
// const/4 v8, 0x0 to discard the real ad list before it reaches the
// coroutine collector. Source file name "BoltDynamicAdFetcher.kt" survives R8.
// ─────────────────────────────────────────────────────────────────────────────

object BoltDynamicAdFetcherInvokeSuspendFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/Object;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("BoltDynamicAdFetcher.kt"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL_RANGE,  // fetchNonLinearAds-hUnOzRk(...)
        Opcode.MOVE_RESULT_OBJECT,    // move-result-object v8
        Opcode.SGET_OBJECT,           // COROUTINE_SUSPENDED sentinel
        Opcode.IF_NE
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass ==
            "Lcom/wbd/adtech/bolt/BoltDynamicAdFetcher\$fetchNonLinearAds\$1;" &&
            methodDef.name == "invokeSuspend"
    }
)

// ─────────────────────────────────────────────────────────────────────────────
// SsaiInfoTimelineBuilder — GMSS/AdSparx SSAI linear ad timeline builder
// Located in classes4.dex
// buildAdBreaksFromAdSparxAdBreaks is the private method that registers
// linear ad breaks with the RangeBuilder. Patched with return-void after
// the annotation block to suppress all SSAI ad break timeline registration.
// access$buildAdBreaksFromAdSparxAdBreaks is the synthetic accessor used
// by inner lambdas. Patched to return-void to close the lambda call path.
// ─────────────────────────────────────────────────────────────────────────────

object SsaiInfoTimelineBuilderBuildAdBreaksFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf(
        "Lcom/discovery/player/common/models/timeline/RangeBuilder\$TimelineBuilder;",
        "Lcom/wbd/beam/player/timelinemanager/timelineprovider/gmsstimelineprovider/gmss/AdSparxTimelineEntry;",
        "Ljava/util/List;"
    ),
    strings = listOf(
        "GssSsaiInfo Parsing failed. Emitting empty timeline with indefinite range",
        "Failed to to deserialize ssai info: "
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "buildAdBreaksFromAdSparxAdBreaks"
    }
)

object SsaiInfoTimelineBuilderAccessorFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC or AccessFlags.FINAL,
    parameters = listOf(
        "Lcom/wbd/beam/player/timelinemanager/timelineprovider/gmsstimelineprovider/GmssTimelineProvider;",
        "Lcom/discovery/player/common/models/timeline/RangeBuilder\$TimelineBuilder;",
        "Lcom/wbd/beam/player/timelinemanager/timelineprovider/gmsstimelineprovider/gmss/AdSparxTimelineEntry;",
        "Ljava/util/List;"
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "access\$buildAdBreaksFromAdSparxAdBreaks"
    }
)
