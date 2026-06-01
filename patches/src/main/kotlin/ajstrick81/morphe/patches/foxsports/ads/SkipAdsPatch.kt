package ajstrick81.morphe.patches.foxsports.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val foxSportsSkipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Suppresses all ad delivery systems in Fox Sports Android TV.",
) {
    execute {
        FoxImaAdEventListenerFingerprint.method.addInstructions(0, "return-void")
        FoxImaAdsLoadedListenerFingerprint.method.addInstructions(0, "return-void")
        FoxPlayerClearVodAdsFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                iput-object v0, p0, Lcom/fox/android/video/player/FoxPlayer;->vodAds:Lcom/fox/android/video/player/args/StreamAds;
                iput-object v0, p0, Lcom/fox/android/video/player/FoxPlayer;->vodAdMarkers:[J
                iput-object v0, p0, Lcom/fox/android/video/player/FoxPlayer;->vodPlayedAdGroups:[Z
            """
        )
        FoxImaVodStreamRequestFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "dai_blocked"
                invoke-interface {p3, v0}, Lcom/fox/android/video/player/loaders/ImaStreamIdLoader${"$"}ImaStreamUrlCallback;->onFailure(Ljava/lang/String;)V
                return-void
            """
        )
        FoxImaLiveStreamRequestFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "dai_blocked"
                invoke-interface {p3, v0}, Lcom/fox/android/video/player/loaders/ImaStreamIdLoader${"$"}ImaStreamIdCallback;->onFailure(Ljava/lang/String;)V
                return-void
            """
        )
        YospaceDispatchAdEventFingerprint.method.addInstructions(0, "return-void")
        YospaceDispatchSlateEventFingerprint.method.addInstructions(0, "return-void")
        YospaceSeekPolicyFingerprint.method.addInstructions(0, "return-void")
    }
}
