package ajstrick81.morphe.patches.future.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.future.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Removes client-side ads in Fawesome Movies & TV (Android TV). The IMA pre/mid-roll " +
        "video ad is suppressed by firing the app's own ad-complete transition (so content loads/resumes " +
        "normally instead of waiting on an ad that never plays); the in-player VAST banner is removed; and " +
        "the pause-screen bug ad is removed while the pause/focus behaviour is preserved. Ads are requested " +
        "client-side, so this is a full removal with no server-stitched (SSAI) fallback to break.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        // ── Hook 1 — IMA video ads (pre/mid-roll), cut at the upstream ad-break ───
        // Force playAd(Z) down the app's OWN no-ad path for every ad break, instead of
        // posting MediaPlayerActivity$7 (which builds the ImaAdsLoader and plays the ad):
        //   • pre-roll (isMidRoll == false): loadVideo(true) — load content now, no ad.
        //   • mid-roll  (isMidRoll == true): return — content is already playing, so just
        //     skip the break (no pause, no ad = seamless).
        // This mirrors playAd's stock :cond_8 branch for ad-free titles, so the activity
        // never enters the ad-waiting state (no freeze, no bounce back to the menu).
        // methodOrNull so a future re-anchor miss degrades gracefully instead of aborting.
        PlayAdFingerprint.methodOrNull?.addInstructionsWithLabels(
            0,
            """
                iget-boolean v0, p0, Lcom/future/moviesByFawesomeAndroidTV/MediaPlayerActivity;->isMidRoll:Z
                if-nez v0, :morphe_noad_ret
                const/4 v0, 0x1
                invoke-virtual {p0, v0}, Lcom/future/moviesByFawesomeAndroidTV/MediaPlayerActivity;->loadVideo(Z)V
                :morphe_noad_ret
                return-void
            """,
        )

        // ── Hook 2 — in-player VAST banner overlay ───────────────────────────────
        // Pure display side effect (Glide-loads the banner, sets isBannerAdVisible).
        // Returning at entry suppresses it with no flow dependency.
        ShowBannerVastAdShowBannerAdFingerprint.methodOrNull?.addInstructions(0, "return-void")

        // ── Hook 3 — pause-screen bug ad overlay ─────────────────────────────────
        // showPauseBugAd() first PAUSES the player (setPlayWhenReady(false)) and moves
        // focus, THEN (if GlobalObject.pauseBanneradbannerVastDto != null) draws the
        // banner. In the ENABLE_ITG=false branches of Playbar.handleKeyPress this method
        // IS the pause handler, so a bare return-void would remove the pause too. Instead
        // keep the pause/focus prologue and return just before the banner rendering — i.e.
        // insert return-void right before the first read of pauseBanneradbannerVastDto.
        PlaybarShowPauseBugAdFingerprint.methodOrNull?.apply {
            val instructions = implementation!!.instructions.toList()
            val dtoReadIndex = instructions.indexOfFirst {
                it.opcode == Opcode.SGET_OBJECT &&
                    ((it as? ReferenceInstruction)?.reference as? FieldReference)?.name == "pauseBanneradbannerVastDto"
            }
            check(dtoReadIndex >= 0) { "skipAdsPatch Hook 3: no pauseBanneradbannerVastDto read in showPauseBugAd" }
            addInstructions(dtoReadIndex, "return-void")
        }
    }
}
