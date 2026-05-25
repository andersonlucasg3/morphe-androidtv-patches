package app.morphe.patches.hbomax.ads

import app.morphe.patcher.Fingerprint

// ─────────────────────────────────────────────────────────────────────────────
// BoltNonLinearAdsRequest — Nonlinear (overlay) ad request serializer
// classes4.dex — kotlinx.serialization descriptor string survives R8
// ─────────────────────────────────────────────────────────────────────────────

internal object BoltNonLinearAdsRequestGetAdRequestTypeFingerprint : Fingerprint(
    strings = listOf("BoltNonLinearAdsRequest(adRequestType="),
    custom = { method, _ ->
        method.definingClass == "Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;" &&
            method.name == "getAdRequestType"
    },
)

internal object BoltNonLinearAdsRequestGetPlaybackIdFingerprint : Fingerprint(
    custom = { method, _ ->
        method.definingClass == "Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;" &&
            method.name == "getPlaybackId"
    },
)

internal object BoltNonLinearAdsRequestWriteSelfFingerprint : Fingerprint(
    strings = listOf("com.wbd.adtech.bolt.BoltNonLinearAdsRequest"),
    custom = { method, _ ->
        method.definingClass == "Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;" &&
            method.name == "write\$Self"
    },
)

// ─────────────────────────────────────────────────────────────────────────────
// BoltDynamicAdFetcher — Nonlinear ad fetch coroutine continuation
// classes4.dex — source file name survives R8
// ─────────────────────────────────────────────────────────────────────────────

internal object BoltDynamicAdFetcherInvokeSuspendFingerprint : Fingerprint(
    strings = listOf("BoltDynamicAdFetcher.kt"),
    custom = { method, _ ->
        method.definingClass ==
            "Lcom/wbd/adtech/bolt/BoltDynamicAdFetcher\$fetchNonLinearAds\$1;" &&
            method.name == "invokeSuspend"
    },
)

// ─────────────────────────────────────────────────────────────────────────────
// SsaiInfoTimelineBuilder — GMSS/AdSparx SSAI linear ad timeline builder
// classes4.dex — SSAI parsing failure strings survive R8
// ─────────────────────────────────────────────────────────────────────────────

internal object SsaiInfoTimelineBuilderBuildAdBreaksFingerprint : Fingerprint(
    strings = listOf("GssSsaiInfo Parsing failed. Emitting empty timeline with indefinite range"),
    custom = { method, _ ->
        method.name == "buildAdBreaksFromAdSparxAdBreaks"
    },
)

internal object SsaiInfoTimelineBuilderAccessorFingerprint : Fingerprint(
    custom = { method, _ ->
        method.name == "access\$buildAdBreaksFromAdSparxAdBreaks"
    },
)

// ─────────────────────────────────────────────────────────────────────────────
// GenerateLiveTimelineEntriesForAdBreakKt — Live stream preroll ad entry builder
// classes.dex — "adBreaks" null check string survives R8
// ─────────────────────────────────────────────────────────────────────────────

internal object GenerateLiveTimelineEntriesForAdBreakFingerprint : Fingerprint(
    strings = listOf("adBreaks"),
    custom = { method, _ ->
        method.definingClass ==
            "Lcom/discovery/adtech/core/models/timeline/GenerateLiveTimelineEntriesForAdBreakKt;" &&
            method.name == "generateLiveTimelineEntriesForAdBreak"
    },
)
