package com.ajstrick81.patches.vix.ads

import app.morphe.patcher.fingerprint

// ---------------------------------------------------------------------------
// Fingerprints for ViX (com.univision.prendetv) v4.46.0_tv
//
// Ad stack summary:
//   • LuraPlayer SDK  – VAST/VMAP orchestration + FreeWheel/IMA wrappers
//   • Innovid SSAI    – JS overlay WebView, SSAIPlaybackState machine
//   • AdsUI           – countdown + skip button UI
//   • VideoPlayerFragment / VideoPlayerController – ad-position ticker
//
// Fingerprinting strategy: RookieEnough-style unique error/log strings that
// survive R8 minification.  Each fingerprint targets the smallest stable
// surface — prefer unique string literals over opcode sequences.
// ---------------------------------------------------------------------------


// ---------------------------------------------------------------------------
// LAYER 1 — LuraPlayer FreeWheel configuration
//
// LuraFreewheelConfiguration holds an `enabled` boolean. The companion
// object's constructor logs "LuraFreewheelConfiguration(enabled=" which is
// unique in the entire DEX. We match on the class init that writes this
// field so we can flip it to false before it's ever read by the scheduler.
// ---------------------------------------------------------------------------
internal val luraFreewheelConfigFingerprint = fingerprint {
    strings("LuraFreewheelConfiguration(enabled=")
}

// ---------------------------------------------------------------------------
// LAYER 2a — LuraAdsConfiguration (VAST/VMAP ad URL and macro bag)
//
// The class serialiser emits "LuraAdsConfiguration(macros=" on toString();
// this is unique to the config data-class and lets us land in the right
// class regardless of R8 class renaming.
// ---------------------------------------------------------------------------
internal val luraAdsConfigFingerprint = fingerprint {
    strings("LuraAdsConfiguration(macros=")
}

// ---------------------------------------------------------------------------
// LAYER 2b — LuraAdsPolicySurrogate (skip-mode policy wrapper)
//
// Controls whether ads are skippable and after how many seconds. The
// surrogate's toString emits "LuraAdsPolicySurrogate(skipMode=" — unique.
// We patch this so the policy always reports the most permissive skip mode.
// ---------------------------------------------------------------------------
internal val luraAdsPolicyFingerprint = fingerprint {
    strings("LuraAdsPolicySurrogate(skipMode=")
}

// ---------------------------------------------------------------------------
// LAYER 3 — Innovid SSAI ad start
//
// InnovidHelper.startAd() is the entry-point for the Innovid WebView overlay.
// Its companion logs "InnovidHelper.kt" as a tag and the method body
// contains "onInnovidAdEvent:" as a callback label — together these two
// strings narrow the match to a single method across all 11 DEX shards.
// ---------------------------------------------------------------------------
internal val innovidStartAdFingerprint = fingerprint {
    strings(
        "InnovidHelper.kt",
        "onInnovidAdEvent:",
    )
}

// ---------------------------------------------------------------------------
// LAYER 4 — AdsUI countdown / showSkippableAd
//
// AdsUI manages the countdown overlay shown during linear ads. The lambda
// class logged as "AdsUI$showSkippableAd$1$2" contains a coroutine delay
// and the unique string "AdsUI$countdown$1" — matching both anchors locks
// us onto the countdown coroutine body that drives the skip timer.
// ---------------------------------------------------------------------------
internal val adsUiCountdownFingerprint = fingerprint {
    strings(
        "AdsUI\$showSkippableAd\$1\$2",
        "AdsUI\$countdown\$1",
    )
}

// ---------------------------------------------------------------------------
// LAYER 5 — VideoPlayerFragment ad update job
//
// VideoPlayerFragment.startAdUpdateJob() launches a coroutine that feeds
// position ticks into the ad engine. Its error path emits the unique string
// "VideoPlayerFragment\$startAdUpdateJob\$1" — a reliable minification-safe
// anchor. Stubbing this prevents all ticker-driven ad-position callbacks.
// ---------------------------------------------------------------------------
internal val videoPlayerAdJobFingerprint = fingerprint {
    strings("VideoPlayerFragment\$startAdUpdateJob\$1")
}

// ---------------------------------------------------------------------------
// AdsOnPause — ViX does NOT appear to ship an AdsOnPause implementation.
//
// Searched all 11 DEX shards for: "pause", "onPause", "adOnPause",
// "pauseAd", "AdOnPause", "onAdPause" in combination with known ad-SDK
// class references. No matching fingerprint surfaces. ViX's Lura/Innovid
// stack pauses ad playback via the ExoPlayer lifecycle directly (the player
// is paused, not an ad surface); there is no discrete AdsOnPause controller
// class to patch. No fingerprint is defined here.
// ---------------------------------------------------------------------------
