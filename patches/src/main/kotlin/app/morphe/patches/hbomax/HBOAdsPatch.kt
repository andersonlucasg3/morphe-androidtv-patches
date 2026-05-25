package app.morphe.patches.hbomax.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.annotation.Patch
import app.morphe.patcher.patch.annotation.Description
import app.morphe.patcher.patch.annotation.CompatiblePackage
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "HBO Max - Disable Ads",
    description = "Suppresses nonlinear overlay ads, SSAI linear ad timeline " +
        "registration, and GMSS/AdSparx ad break construction for VOD content.",
    compatiblePackages = [
        CompatiblePackage(
            name = "com.hbo.hbonow",
            versions = ["6.17.1.4"]
        )
    ],
    use = true
)
@Suppress("unused")
object HBOAdsPatch : BytecodePatch(
    setOf(
        BoltNonLinearAdsRequestGetAdRequestTypeFingerprint,
        BoltNonLinearAdsRequestGetPlaybackIdFingerprint,
        BoltNonLinearAdsRequestWriteSelfFingerprint,
        BoltDynamicAdFetcherInvokeSuspendFingerprint,
        SsaiInfoTimelineBuilderBuildAdBreaksFingerprint,
        SsaiInfoTimelineBuilderAccessorFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        // ─────────────────────────────────────────────────────────────────────
        // Patch 1: BoltNonLinearAdsRequest.getAdRequestType()
        // Was missing method name and used .registers 1 (no v0 slot).
        // Replace entire body to return empty string.
        // ─────────────────────────────────────────────────────────────────────
        BoltNonLinearAdsRequestGetAdRequestTypeFingerprint.result!!.mutableMethod.apply {
            addInstructions(
                0,
                """
                    const-string v0, ""
                    return-object v0
                """
            )
        }

        // ─────────────────────────────────────────────────────────────────────
        // Patch 2: BoltNonLinearAdsRequest.getPlaybackId()
        // Was returning const/4 v0, 0x0 (null) which NPEs on @NotNull callers.
        // Replace with empty string return.
        // ─────────────────────────────────────────────────────────────────────
        BoltNonLinearAdsRequestGetPlaybackIdFingerprint.result!!.mutableMethod.apply {
            addInstructions(
                0,
                """
                    const-string v0, ""
                    return-object v0
                """
            )
        }

        // ─────────────────────────────────────────────────────────────────────
        // Patch 3: BoltNonLinearAdsRequest.write$Self()
        // Suppress advertisingInfo (field index 2) from the serialized JSON
        // body entirely — omitting the key rather than nulling it avoids NPE
        // in the kotlinx.serialization encoder.
        // Replace playbackId (field index 5) with an empty string constant.
        // Fields 0 (adRequestType), 1 (adContext), 3 (deviceInfo),
        // 4 (capabilities), and 6 (editId) are left intact so the request
        // remains structurally valid and the Bolt server returns a graceful
        // empty / 4xx response rather than a hard crash.
        // ─────────────────────────────────────────────────────────────────────
        BoltNonLinearAdsRequestWriteSelfFingerprint.result!!.mutableMethod.apply {
            // Clear all existing instructions and rewrite the method body
            clearInstructions()
            addInstructionsWithLabels(
                0,
                """
                    # Load ${"$"}childSerializers array (needed for capabilities at index 4)
                    sget-object v0, Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;->${"$"}childSerializers:[Lkotlinx/serialization/KSerializer;

                    # [0] adRequestType — keep (structural, not user-identifying)
                    const/4 v1, 0x0
                    iget-object v2, p0, Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;->adRequestType:Ljava/lang/String;
                    invoke-interface {p1, p2, v1, v2}, Lbr/d;->o(Lar/f;ILjava/lang/String;)V

                    # [1] adContext — keep (content context, structural)
                    sget-object v1, Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest${"$"}AdContext${"$$"}serializer;->INSTANCE:Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest${"$"}AdContext${"$$"}serializer;
                    iget-object v2, p0, Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;->adContext:Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest${"$"}AdContext;
                    const/4 v3, 0x1
                    invoke-interface {p1, p2, v3, v1, v2}, Lbr/d;->E(Lar/f;ILyq/o;Ljava/lang/Object;)V

                    # [2] advertisingInfo — SUPPRESSED
                    # Field omitted entirely. JSON encoder produces no key.
                    # Bolt server receives request without ADID/targeting payload.

                    # [3] deviceInfo — keep (platform type, non-identifying)
                    sget-object v1, Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest${"$"}DeviceInfo${"$$"}serializer;->INSTANCE:Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest${"$"}DeviceInfo${"$$"}serializer;
                    iget-object v2, p0, Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;->deviceInfo:Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest${"$"}DeviceInfo;
                    const/4 v3, 0x3
                    invoke-interface {p1, p2, v3, v1, v2}, Lbr/d;->E(Lar/f;ILyq/o;Ljava/lang/Object;)V

                    # [4] capabilities — keep (client flags, structural)
                    const/4 v1, 0x4
                    aget-object v0, v0, v1
                    iget-object v2, p0, Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;->capabilities:Ljava/util/List;
                    invoke-interface {p1, p2, v1, v0, v2}, Lbr/d;->E(Lar/f;ILyq/o;Ljava/lang/Object;)V

                    # [5] playbackId — SUPPRESSED (empty string, safe for o() encoder)
                    const/4 v0, 0x5
                    const-string v1, ""
                    invoke-interface {p1, p2, v0, v1}, Lbr/d;->o(Lar/f;ILjava/lang/String;)V

                    # [6] editId — keep (content variant ID, not user-identifying)
                    const/4 v0, 0x6
                    iget-object p0, p0, Lcom/wbd/adtech/bolt/BoltNonLinearAdsRequest;->editId:Ljava/lang/String;
                    invoke-interface {p1, p2, v0, p0}, Lbr/d;->o(Lar/f;ILjava/lang/String;)V

                    return-void
                """
            )
        }

        // ─────────────────────────────────────────────────────────────────────
        // Patch 4: BoltDynamicAdFetcher$fetchNonLinearAds$1.invokeSuspend()
        // After fetchNonLinearAds returns its result into v8, insert
        // const/4 v8, 0x0 to discard the real ad list before it reaches
        // the coroutine collector. null != COROUTINE_SUSPENDED so the
        // if-ne branch is taken, Result.success(null) is returned, and
        // the GMSS caller receives a null List<NonLinearAd>? — no ads
        // are scheduled, no crash occurs.
        // ─────────────────────────────────────────────────────────────────────
        BoltDynamicAdFetcherInvokeSuspendFingerprint.result!!.let { result ->
            val method = result.mutableMethod
            val instructions = method.implementation!!.instructions

            // Find the MOVE_RESULT_OBJECT that follows the fetchNonLinearAds call
            val moveResultIndex = instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.MOVE_RESULT_OBJECT &&
                    instructions.indexOf(instruction) > 0 &&
                    instructions[instructions.indexOf(instruction) - 1].opcode ==
                    Opcode.INVOKE_VIRTUAL_RANGE
            }

            // Insert const/4 v8, 0x0 immediately after move-result-object v8
            // v8 is the result register in the 9-register frame
            method.addInstructions(
                moveResultIndex + 1,
                "const/4 v8, 0x0"
            )
        }

        // ─────────────────────────────────────────────────────────────────────
        // Patch 5: SsaiInfoTimelineBuilder.buildAdBreaksFromAdSparxAdBreaks()
        // Insert return-void at method entry (index 0) before any ad break
        // data is read or written to the RangeBuilder$TimelineBuilder.
        // The .locals 16 declaration is untouched — execution never reaches
        // any instruction that uses those slots, so no register validation
        // error is produced by the assembler.
        // ─────────────────────────────────────────────────────────────────────
        SsaiInfoTimelineBuilderBuildAdBreaksFingerprint.result!!.mutableMethod.apply {
            addInstructions(0, "return-void")
        }

        // ─────────────────────────────────────────────────────────────────────
        // Patch 6: SsaiInfoTimelineBuilder.access$buildAdBreaksFromAdSparxAdBreaks()
        // Synthetic accessor used by buildTimeline$timeline$1 and
        // buildTimeline$timeline$2 inner lambdas. Remove the invoke-direct
        // call and leave only return-void so the lambda call path is also
        // suppressed. .locals 0 is correct — no registers are needed.
        // ─────────────────────────────────────────────────────────────────────
        SsaiInfoTimelineBuilderAccessorFingerprint.result!!.mutableMethod.apply {
            clearInstructions()
            addInstructions(0, "return-void")
        }
    }
}
