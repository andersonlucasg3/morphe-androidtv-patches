package ajstrick81.morphe.extension.tubi.ads;

/**
 * Tubi — FoxPlayer/FoxIMA ad suppression extension.
 *
 * Hook 1 (primary) is handled entirely in SkipAdsPatch.kt via returnEarly() —
 * no Java extension needed for that since it inserts return-void directly.
 *
 * This extension handles Hook 2 — amplifying FoxPlayer.clearVodAds() to also
 * null out the IMA StreamManager reference in FoxImaAdListeners, preventing
 * any stale IMA session from reactivating ad delivery after a clearVodAds call.
 *
 * FoxPlayer is passed as Object to avoid needing a stub class. The IMA
 * StreamManager null-out is done via reflection for the same reason.
 *
 * All operations are wrapped in try/catch — any failure is a silent no-op
 * so playback continues normally.
 */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class SkipAdsPatch {

    /**
     * Called at index 0 of FoxPlayer.clearVodAds().
     *
     * Attempts to find the FoxImaAdListeners instance held by FoxPlayer and
     * null out its imaStreamManager field. This prevents any IMA StreamManager
     * that was initialized before our Hook 1 fired from delivering ad events
     * via a retained reference.
     *
     * FoxPlayer holds FoxImaAdListeners via the FoxImaAdsLoader chain:
     *   FoxPlayer → FoxImaAdsLoader → FoxImaStreamIdLoader → FoxImaAdListeners
     *
     * If the reflection chain fails at any point we return silently — the
     * returnEarly() on the adEventListener lambda is the primary suppression
     * mechanism and this is a belt-and-suspenders cleanup only.
     *
     * @param foxPlayer The FoxPlayer instance (typed as Object to avoid stub).
     */
    public static void onClearVodAds(Object foxPlayer) {
        if (foxPlayer == null) return;
        try {
            // Walk the object graph: FoxPlayer → imaAdsLoader field
            // Field names are not obfuscated in FoxPlayer (Fox's own code)
            java.lang.reflect.Field adsLoaderField =
                foxPlayer.getClass().getDeclaredField("imaAdsLoader");
            adsLoaderField.setAccessible(true);
            Object adsLoader = adsLoaderField.get(foxPlayer);
            if (adsLoader == null) return;

            // FoxImaAdsLoader → imaAdListeners field
            java.lang.reflect.Field listenersField =
                adsLoader.getClass().getDeclaredField("imaAdListeners");
            listenersField.setAccessible(true);
            Object listeners = listenersField.get(adsLoader);
            if (listeners == null) return;

            // FoxImaAdListeners → imaStreamManager field — null it out
            java.lang.reflect.Field managerField =
                listeners.getClass().getDeclaredField("imaStreamManager");
            managerField.setAccessible(true);
            managerField.set(listeners, null);

        } catch (Exception e) {
            // Silent fail — Hook 1 returnEarly() is the primary suppression
        }
    }
}
