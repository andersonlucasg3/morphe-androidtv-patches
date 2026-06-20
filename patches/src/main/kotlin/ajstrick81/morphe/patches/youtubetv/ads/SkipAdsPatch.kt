package ajstrick81.morphe.patches.youtubetv.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.youtubetv.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Suppresses video ads and ad overlays in YouTube Android TV.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {

        // ─────────────────────────────────────────────────────────────────────
        // Hook 1 — OS Name Spoofing
        //
        // Replaces the OS name in the InnerTube client context with
        // "Android Automotive". YouTube does not serve video ads on the
        // Android Automotive platform (stricter automotive ad policies).
        //
        // Ported from Morphe mobile YouTube: hideVideoAds(String osName)
        // returning "Android Automotive".
        //
        // Evidence: "ANDROID_TV" (3 DEX matches), "Android", "clientName",
        //           "platformDetails" confirmed in APK v7.05.301.
        // ─────────────────────────────────────────────────────────────────────
        YouTubeTvOsNameFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "Android Automotive"
                return-object v0
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 2 — VAST/VMAP Ad Request Short-Circuit
        //
        // Short-circuits the video ad request method. YouTube TV fetches
        // ads via VAST/VMAP protocol, not the slot/adapter system used
        // in mobile YouTube. Setting a 0 return prevents the ad manifest
        // from being loaded.
        //
        // Evidence: "vastAdsRequest" confirmed in APK v7.05.301.
        // ─────────────────────────────────────────────────────────────────────
        YouTubeTvLoadVideoAdsFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 3 — Ad Break Parser Short-Circuit
        //
        // Returns an empty ArrayList from the ad break parser to prevent
        // any ad breaks from being scheduled. Pattern from Disney's
        // Insertion.getPoints/getRanges.
        //
        // Evidence: "adBreak" (3 DEX matches), "AdBreakClipInfo" confirmed
        //           in APK v7.05.301.
        // ─────────────────────────────────────────────────────────────────────
        YouTubeTvAdBreakParserFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Ljava/util/ArrayList;
                invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                return-object v0
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 4 — Coroutine Ad Fetch Stall
        //
        // Returns COROUTINE_SUSPENDED sentinel to stall the Kotlin coroutine
        // that fetches VAST/VMAP ad manifests. The coroutine suspends, the
        // ad manifest never arrives, and YouTube's timeout proceeds to
        // play content without pre-roll ads.
        //
        // Pattern from Tubi Hook 9 (RainmakerSuspendGetAdBreaksFingerprint).
        //
        // Evidence: "vastAdsRequest" + COROUTINE_SUSPENDED confirmed in APK.
        //           Uses Continuation parameter detection (suspend function).
        // ─────────────────────────────────────────────────────────────────────
        YouTubeTvSuspendGetAdsFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->COROUTINE_SUSPENDED:Ljava/lang/Object;
                return-object v0
            """
        )

    }
}
