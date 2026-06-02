/*
 * Paramount+ Android TV — Ad Suppression Patch
 *
 * Validated against:
 *   v16.8.0  (versionCode 520000464) — com.cbs.ott
 *   v16.12.0 (versionCode 520000571) — com.cbs.ott
 *
 * What this patch covers:
 *   ✅ Pause ads (CbsPauseWithAdsOverlay)
 *
 * Pending (requires further APK analysis):
 *   ⏳ VOD SSAI ads — needs IMA StreamManager ad break interception
 *   ⏳ Live TV ads  — needs SSAI segment-level or StreamManager approach
 *
 * VOD SSAI NOTE:
 *   createVodStreamRequest patching was attempted but breaks VOD playback
 *   entirely. Paramount+ VOD is pure SSAI — the content manifest URL is
 *   returned by the DAI exchange, not stored independently in the app.
 *   An empty StreamRequest causes the IMA SDK to error out asynchronously,
 *   which the app's error handler responds to with a black screen.
 *   The correct interception point is the IMA StreamManager's ad break
 *   event chain (onAdBreakStarted / StreamEvent.Type.AD_BREAK_STARTED),
 *   which fires during playback after the content URL is already loaded.
 *
 * LIVE TV NOTE:
 *   createLiveStreamRequest is intentionally NOT patched.
 *   The asset key set by that method is required to obtain the stitched
 *   HLS manifest URL from saa.paramountplus.com — patching it produces
 *   a black screen. Live TV ad suppression requires the same StreamManager
 *   interception approach as VOD.
 */

package app.morphe.patches.paramount

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val paramountPatch = bytecodePatch(
    name = "Paramount+ Android TV",
    description = "Removes pause ads.",
) {
    compatibleWith(AppCompatibilities.PARAMOUNT_TV)

    execute {
        // ------------------------------------------------------------------
        // Patch 1: Pause ads — CbsPauseWithAdsOverlay state machine
        //
        // This public static method dispatches sealed state objects to
        // render the pause ad overlay via Glide image loading and a
        // 1000ms alpha fade-in animation.
        //
        // Patching strategy: return-void at offset 0.
        //   - No Glide network request for the ad image is ever made
        //   - The alpha fade-in to 1.0 is never triggered
        //   - The overlay view stays at alpha=0 (invisible) during pause
        //   - No NPE risk — the overlay View and its key event handler
        //     are constructed independently and are unaffected
        //
        // The method name is obfuscated (P in v16.8.0, M in v16.12.0)
        // so the patch resolves via the PauseAdOverlayFingerprint string
        // anchors rather than by name. See Fingerprints.kt for details.
        // ------------------------------------------------------------------
        PauseAdOverlayFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
