package ajstrick81.morphe.patches.future.ads

import app.morphe.patcher.Fingerprint

// ─────────────────────────────────────────────────────────────────────────────
// Fawesome Movies & TV — com.future.moviesByFawesomeAndroidTV 19.7 — ad display seams.
//
// This is a client-side-ads app (Future Today Inc.). Ads reach the screen through
// two independent display pathways, both driven from app bytecode:
//
//   A. IMA video ads (pre-roll / mid-roll): AdsPlayer.requestAds -> showMidRollAd,
//      which builds a media3 ImaAdsLoader from an ad-tag URL and plays the ad.
//   B. VAST "banner" / "bug" overlay ads: image overlays driven by
//      GlobalObject.bannerVastDto / pauseBanneradbannerVastDto, drawn by
//      ShowBannerVastAd.showBannerAd (in-player banner) and Playbar.showPauseBugAd
//      (pause-screen bug ad).
//
// Each hook targets the single display entry point for its ad type, so the ad is
// never shown. Class/method names are NOT R8-obfuscated in this build (com/future/*
// is preserved), but we still anchor on the stable .source file name + method name
// so a future rebuild can be re-found with the seam-map --ref trick.
// ─────────────────────────────────────────────────────────────────────────────

// Hook 1 — Lcom/future/moviesByFawesomeAndroidTV/MediaPlayerActivity;->playAd(Z)V
// The upstream ad-break entry point. When an ad break is due, playAd() sets
// isAdsVisible=true and posts MediaPlayerActivity$7 (which runs AdsPlayer.initAds ->
// requestAds -> showMidRollAd to build the ImaAdsLoader and play the ad). playAd()
// ALSO contains the app's own no-ad branch (:cond_8): for a title with no ad data it
// pre-rolls straight to content (loadVideo(true)) and, for a mid-roll, simply returns
// (content keeps playing). The patch forces that exact no-ad path for EVERY ad break,
// so no ad is ever requested or shown and content loads/resumes natively. This is why
// hooking here (not showMidRollAd) avoids both the freeze-on-switch and the
// bounce-to-menu: we never enter the ad-waiting state at all. isMidRoll is the field
// playAd itself reads to choose loadVideo vs. return.
object PlayAdFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Z"),
    custom = { method, classDef ->
        method.name == "playAd" &&
        classDef.type.endsWith("/MediaPlayerActivity;")
    },
)

// Hook 2 — Lcom/future/util/ShowBannerVastAd;->showBannerAd(Landroid/content/Context;Landroid/widget/ImageView;Landroid/widget/ImageView;Landroidx/media3/ui/PlayerView;Lcom/future/dto/Object_data;)V
// Draws the in-player VAST banner overlay (loads the banner image into the overlay
// ImageView and marks GlobalObject.isBannerAdVisible). Returning void suppresses the
// banner; it is a pure display side effect.
object ShowBannerVastAdShowBannerAdFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, classDef ->
        method.name == "showBannerAd" &&
        classDef.type.endsWith("/ShowBannerVastAd;")
    },
)

// Hook 3 — Lcom/future/customviews/Playbar;->showPauseBugAd()V
// Draws the pause-screen "bug" ad overlay from GlobalObject.pauseBanneradbannerVastDto.
// The method pauses the player + moves focus BEFORE the banner draw, and in the
// ENABLE_ITG=false branches of handleKeyPress this method is the pause handler itself.
// So the patch preserves the pause/focus prologue and returns just before the banner
// rendering (the first read of pauseBanneradbannerVastDto) — not a bare entry return.
object PlaybarShowPauseBugAdFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, classDef ->
        method.name == "showPauseBugAd" &&
        classDef.type.endsWith("/Playbar;")
    },
)
