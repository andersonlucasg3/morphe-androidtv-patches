package ajstrick81.morphe.extension.primevideo.ads;

import android.util.Log;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Prime Video ATV — ad suppression extension.
 *
 * Entry points called from patched bytecode:
 *
 *   skipAllMedia3AdGroups — strips ad schedule from media3 AdPlaybackState map
 *   skipAllExo2AdGroups   — same for ExoPlayer2 / GMS Ads variant
 *
 * Note: Hook 4 (MetricsTransporter.transmit) uses pure inline smali to
 * construct a fake UploadResult("SUCCESS", "ok") directly — no Java extension
 * method needed since UploadResult lives in the app's own DEX.
 *
 * --- AdPlaybackState suppression (Hooks 1 & 2) ---
 *
 * withRemovedAdGroupCount(adGroupCount) physically removes all ad groups
 * from the AdPlaybackState before ExoPlayer sees the map. Operates at the
 * SSAI schedule layer — primary suppression for standard ad delivery.
 *
 * Confirmed via dex disassembly (June 2026) that there is no separate
 * Java-level pipeline for the PromoPlaybackExperience WASM ad path — no
 * ImaAdsLoader/CSAI is present in the app, and the only other
 * AdPlaybackState setter (SharedMediaPeriod.updateAdPlaybackState) has zero
 * call sites in the app's own bytecode. Promo-sourced ads still funnel
 * through this same setAdPlaybackStates entry point. The previously-shipped
 * "seekToAdBreakEnd" hook targeted a getStreamPositionUs(Player,
 * AdPlaybackState) overload that also has zero call sites and was removed
 * as dead code — it never executed and was not providing the
 * PromoPlaybackExperience coverage its comments claimed.
 *
 * All methods wrapped in try/catch — failures are logged to TAG below instead
 * of silently swallowed, so logcat can confirm whether the hook fired and
 * whether the strip actually took effect.
 */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class SkipAdsPatch {

    private static final String TAG = "SkipAdsPatch";

    // ── AdPlaybackState suppression ───────────────────────────────────────────

    /**
     * Transforms an AdPlaybackState map for the media3 SSAI pipeline.
     *
     * Called at index 0 of:
     *   androidx.media3.exoplayer.source.ads.ServerSideAdInsertionMediaSource
     *       .setAdPlaybackStates(ImmutableMap, Timeline)
     */
    public static ImmutableMap skipAllMedia3AdGroups(ImmutableMap adPlaybackStates) {
        try {
            ImmutableMap.Builder builder = ImmutableMap.builder();
            int strippedGroups = 0;
            for (Object o : adPlaybackStates.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                Object key = entry.getKey();
                androidx.media3.common.AdPlaybackState state =
                        (androidx.media3.common.AdPlaybackState) entry.getValue();
                if (state.adGroupCount > state.removedAdGroupCount) {
                    strippedGroups += state.adGroupCount - state.removedAdGroupCount;
                    state = state.withRemovedAdGroupCount(state.adGroupCount);
                }
                builder.put(key, state);
            }
            Log.i(TAG, "skipAllMedia3AdGroups: entries=" + adPlaybackStates.size()
                    + " strippedGroups=" + strippedGroups);
            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "skipAllMedia3AdGroups failed", e);
            return adPlaybackStates;
        }
    }

    /**
     * Transforms an AdPlaybackState map for the ExoPlayer2 SSAI pipeline
     * bundled inside the Google Mobile Ads SDK (classes4.dex).
     *
     * Called at index 0 of:
     *   com.google.android.exoplayer2.source.ads.ServerSideAdInsertionMediaSource
     *       .setAdPlaybackStates(ImmutableMap)
     */
    public static ImmutableMap skipAllExo2AdGroups(ImmutableMap adPlaybackStates) {
        try {
            ImmutableMap.Builder builder = ImmutableMap.builder();
            int strippedGroups = 0;
            for (Object o : adPlaybackStates.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                Object key = entry.getKey();
                com.google.android.exoplayer2.source.ads.AdPlaybackState state =
                        (com.google.android.exoplayer2.source.ads.AdPlaybackState) entry.getValue();
                if (state.adGroupCount > state.removedAdGroupCount) {
                    strippedGroups += state.adGroupCount - state.removedAdGroupCount;
                    state = state.withRemovedAdGroupCount(state.adGroupCount);
                }
                builder.put(key, state);
            }
            Log.i(TAG, "skipAllExo2AdGroups: entries=" + adPlaybackStates.size()
                    + " strippedGroups=" + strippedGroups);
            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "skipAllExo2AdGroups failed", e);
            return adPlaybackStates;
        }
    }

}
