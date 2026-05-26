package ajstrick81.morphe.extension.primevideo.ads;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class SkipAdsPatch {

    /**
     * Transforms an AdPlaybackState map for the media3 SSAI pipeline by calling
     * withSkippedAdGroup() on every active ad group in each state before it is
     * validated and posted to the ExoPlayer playback thread.
     *
     * Called at index 0 of:
     *   androidx.media3.exoplayer.source.ads.ServerSideAdInsertionMediaSource
     *       .setAdPlaybackStates(ImmutableMap, Timeline)
     *
     * Loop range: [removedAdGroupCount, adGroupCount)
     *   - removedAdGroupCount: groups already passed (live streams only); skipping
     *     below this index would produce a negative array index inside withSkippedAdGroup.
     *   - adGroupCount: total group count including removed groups; active groups
     *     live at array indices [0, adGroupCount - removedAdGroupCount).
     *
     * withSkippedAdGroup(i) internally does: arrayIndex = i - removedAdGroupCount,
     * then calls AdGroup.withAllAdsSkipped() which sets AD_STATE_SKIPPED on every
     * individual ad in the group. It does not touch isServerSideInserted, so the
     * original validation in setAdPlaybackStates continues to pass.
     *
     * Fail-safe: any exception returns the original map unmodified so playback
     * degrades gracefully rather than crashing.
     */
    public static ImmutableMap skipAllMedia3AdGroups(ImmutableMap adPlaybackStates) {
        try {
            ImmutableMap.Builder builder = ImmutableMap.builder();
            for (Object o : adPlaybackStates.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                Object key = entry.getKey();
                androidx.media3.common.AdPlaybackState state =
                        (androidx.media3.common.AdPlaybackState) entry.getValue();
                for (int i = state.removedAdGroupCount; i < state.adGroupCount; i++) {
                    state = state.withSkippedAdGroup(i);
                }
                builder.put(key, state);
            }
            return builder.build();
        } catch (Exception e) {
            return adPlaybackStates;
        }
    }

    /**
     * Same transformation for the ExoPlayer2 SSAI pipeline bundled inside
     * the Google Mobile Ads SDK (com.google.android.gms.internal.ads, classes3.dex).
     *
     * Called at index 0 of:
     *   com.google.android.exoplayer2.source.ads.ServerSideAdInsertionMediaSource
     *       .setAdPlaybackStates(ImmutableMap)
     *
     * The ExoPlayer2 AdPlaybackState API is structurally identical to the media3
     * version — same public fields (removedAdGroupCount, adGroupCount) and the same
     * withSkippedAdGroup(int) contract — so the transformation logic is the same.
     */
    public static ImmutableMap skipAllExo2AdGroups(ImmutableMap adPlaybackStates) {
        try {
            ImmutableMap.Builder builder = ImmutableMap.builder();
            for (Object o : adPlaybackStates.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                Object key = entry.getKey();
                com.google.android.exoplayer2.source.ads.AdPlaybackState state =
                        (com.google.android.exoplayer2.source.ads.AdPlaybackState) entry.getValue();
                for (int i = state.removedAdGroupCount; i < state.adGroupCount; i++) {
                    state = state.withSkippedAdGroup(i);
                }
                builder.put(key, state);
            }
            return builder.build();
        } catch (Exception e) {
            return adPlaybackStates;
        }
    }
}
