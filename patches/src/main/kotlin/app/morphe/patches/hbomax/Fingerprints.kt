package app.morphe.patches.hbomax.ads

import app.morphe.patcher.Fingerprint

// ─────────────────────────────────────────────────────────────────────────────
// BoltNonLinearAdsRequest — Nonlinear (overlay) ad request serializer
// classes4.dex — kotlinx.serialization descriptor string survives R8.
// write$Self is the serialization write path — suppresses advertisingInfo
// (field index 2) and zeroes playbackId (field index 5) from the JSON body.
// ─────────────────────────────────────────────────────────────────────────────

internal object BoltNonLinearAdsRequestWriteSelfFingerprint : Fingerprint(
    strings = listOf("com.wbd.adtech.bolt.BoltNonLinearAdsRequest"),
    custom = { method, _ ->
        method.definingClass == "Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;" &&
            method.name == "write\$Self"
    },
)

// ─────────────────────────────────────────────────────────────────────────────
// BoltDynamicAdFetcher — Nonlinear ad fetch coroutine continuation
// classes4.dex — source file name survives R8.
// invokeSuspend discards the real ad list after fetchNonLinearAds returns,
// causing the coroutine collector to receive null instead of a populated list.
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
// classes4.dex — SSAI parsing failure string survives R8.
// buildAdBreaksFromAdSparxAdBreaks registers linear ad breaks with the
// RangeBuilder. Patched with return-void to suppress all SSAI ad break
// timeline registration for VOD and movies.
// access$buildAdBreaksFromAdSparxAdBreaks is the synthetic accessor used
// by buildTimeline inner lambdas — patched to close that call path too.
// ─────────────────────────────────────────────────────────────────────────────

internal object SsaiInfoTimelineBuilderBuildAdBreaksFingerprint : Fingerprint(
    strings = listOf("GSsaiInfo Parsing failed. Emitting empty timeline with indefinite range"),
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
// classes.dex — "adBreaks" null check string survives R8.
// Returns empty ArrayList instead of building AdBreakEntry/AdEntry objects.
// The caller (generateLiveTimelineEntries) does addAll() on the result —
// empty list means no ad entries added to live timeline while chapter/content
// entries are built normally. Suppresses "1 of 2" countdown prerolls on
// live and episodic TV content.
// ─────────────────────────────────────────────────────────────────────────────

internal object GenerateLiveTimelineEntriesForAdBreakFingerprint : Fingerprint(
    strings = listOf("adBreaks"),
    custom = { method, _ ->
        method.definingClass ==
            "Lcom/discovery/adtech/core/models/timeline/GenerateLiveTimelineEntriesForAdBreakKt;" &&
            method.name == "generateLiveTimelineEntriesForAdBreak"
    },
)
