package ajstrick81.morphe.patches.youtubetv.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Ad-related method fingerprints for YouTube Android TV.
 *
 * ALL FINGERPRINTS REQUIRE PHASE 0 REVERSE ENGINEERING.
 * String values and class names are educated guesses based on the
 * Morphe mobile YouTube patches and ReVanced's LoadVideoAdsFingerprint.
 * Must be verified against the actual com.google.android.youtube.tv APK.
 */

// Hook 1 — OS Name Spoofing
//
// Ported from Morphe mobile YouTube: replaces the OS name in the InnerTube
// client context with "Android Automotive". YouTube does not serve video ads
// on the Android Automotive platform (stricter ad policies for automotive).
//
// In mobile, the method is hideVideoAds(String osName) which returns the
// spoofed OS name. The equivalent in the TV APK likely constructs the
// InnerTube client context protobuf and sets the os_name/platform field.
//
// Expected fingerprint: method returning String containing "Android" constant
// Strategy: strings("Android") + custom (returnType = String, classDef.type check)
object YouTubeTvOsNameFingerprint : Fingerprint(
    strings = listOf("Android"),
    custom = { method, classDef ->
        // TODO Phase 0: Find the exact method that returns/provides the OS name
        // in the InnerTube client context. Target class may contain:
        // "InnerTube", "ClientInfo", "context", "DeviceInfo", "Platform"
        method.returnType == "Ljava/lang/String;" &&
            (classDef.type.contains("InnerTube") ||
             classDef.type.contains("ClientInfo") ||
             classDef.type.contains("context") ||
             classDef.type.contains("DeviceInfo") ||
             classDef.type.contains("Platform"))
    },
)

// Hook 2 — Video Ad Loading Interception
//
// Ported from ReVanced's LoadVideoAdsFingerprint. This method is responsible
// for loading and playing video ads (pre-roll, mid-roll).
//
// ReVanced uses these distinctive string constants:
// - "TriggerBundle doesn't have the required metadata specified by the trigger "
// - "Tried to enter slot with no assigned slotAdapter"
// - "Trying to enter a slot when a slot of same type and physical position is already active. Its status: "
//
// These strings are from YouTube's slot/adapter system and are likely shared
// between mobile and TV codebases since they're in core YouTube libraries.
//
// Strategy: strings(the three ReVanced strings) + custom (returnType check)
// Action: addInstructions(0, smali) to return early — skip ad loading
object YouTubeTvLoadVideoAdsFingerprint : Fingerprint(
    strings = listOf(
        "TriggerBundle doesn't have the required metadata specified by the trigger ",
        "Tried to enter slot with no assigned slotAdapter",
        "Trying to enter a slot when a slot of same type and physical position is already active. Its status: ",
    ),
    custom = { method, _ ->
        // TODO Phase 0: Verify these strings exist in the TV APK.
        // The slot/adapter system should be shared between mobile and TV.
        // If they don't match, search for "adBreak" or "adPlacement" strings.
        method.returnType != "V" // likely returns some ad-related object
    },
)

// Hook 3 — Ad Break Parser Short-Circuit
//
// Finds the method that parses ad break data from the InnerTube player
// response. Returns an empty list to prevent ad breaks from being scheduled.
// Pattern: similar to Disney Insertion.getPoints/getRanges
//
// Expected strings: "adBreak", "adPlacement", or "playerAds"
// Action: return empty ArrayList
object YouTubeTvAdBreakParserFingerprint : Fingerprint(
    strings = listOf("adBreak"),
    custom = { method, _ ->
        // TODO Phase 0: Find exact method parsing ad breaks from InnerTube response
        method.returnType?.contains("List") == true ||
            method.returnType?.contains("Collection") == true
    },
)

// Hook 4 — Ad Fetch Coroutine Suspension
//
// If YouTube TV uses Kotlin coroutines for ad fetching (likely in modern
// versions), return COROUTINE_SUSPENDED sentinel to stall ad requests.
// Pattern: identical to Tubi Hook 9 (RainmakerSuspendGetAdBreaksFingerprint)
//
// Expected: method with Continuation in parameters, containing ad-related strings
// Action: sget-object v0, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->COROUTINE_SUSPENDED:Ljava/lang/Object; return-object v0
object YouTubeTvSuspendGetAdsFingerprint : Fingerprint(
    strings = listOf("fetchAd", "getAd"),
    custom = { method, _ ->
        // TODO Phase 0: Find coroutine-based ad fetching methods
        // Must have Continuation in parameters
        method.parameterTypes.any {
            it.contains("Continuation")
        }
    },
)

// Hook 5 — Ad State Machine Transition Interception
//
// If YouTube TV uses a state machine to transition into ad playback mode,
// short-circuit the enter-ad-break transition.
// Pattern: similar to Prime Video's seek technique or HBO Max's state machine patches
//
// Strategy: custom lambda with opcode pattern matching (injected in SkipAdsPatch.kt)
// Action: return-void at index 0 of state transition method
object YouTubeTvAdStateTransitionFingerprint : Fingerprint(
    strings = listOf("ad", "break", "transition"),
    custom = { method, _ ->
        // TODO Phase 0: Find ad state machine transition method
        method.returnType == "V"
    },
)
