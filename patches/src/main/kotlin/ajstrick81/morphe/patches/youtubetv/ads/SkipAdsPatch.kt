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
        // Status: PLACEHOLDER — requires Phase 0 RE to find exact method
        // ─────────────────────────────────────────────────────────────────────
        YouTubeTvOsNameFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "Android Automotive"
                return-object v0
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 2 — Video Ad Loading Interception
        //
        // Short-circuits the video ad loading method. Based on ReVanced's
        // LoadVideoAdsFingerprint which uses distinctive slot/adapter strings.
        //
        // Status: PLACEHOLDER — requires Phase 0 RE to verify strings exist
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
        // Status: PLACEHOLDER — requires Phase 0 RE to find exact method
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
        // Hook 4 — Ad Fetch Coroutine Suspension
        //
        // Returns COROUTINE_SUSPENDED sentinel to stall Kotlin coroutine-based
        // ad fetching. The coroutine suspends, the ad manifest never arrives,
        // and YouTube's timeout proceeds to play content.
        //
        // Pattern from Tubi Hook 9 (RainmakerSuspendGetAdBreaksFingerprint).
        //
        // Status: PLACEHOLDER — requires Phase 0 RE to find exact method
        // ─────────────────────────────────────────────────────────────────────
        YouTubeTvSuspendGetAdsFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->COROUTINE_SUSPENDED:Ljava/lang/Object;
                return-object v0
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 5 — Ad State Machine Transition Interception
        //
        // Short-circuits the transition into ad playback mode in YouTube's
        // player state machine. Prevents the player from entering the
        // ad-playing state entirely.
        //
        // Status: PLACEHOLDER — requires Phase 0 RE to find exact method
        // ─────────────────────────────────────────────────────────────────────
        YouTubeTvAdStateTransitionFingerprint.method.addInstructions(
            0,
            "return-void"
        )
    }
}
