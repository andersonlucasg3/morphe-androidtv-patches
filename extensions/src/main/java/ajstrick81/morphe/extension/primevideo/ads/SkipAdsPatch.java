package ajstrick81.morphe.extension.primevideo.ads;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Prime Video ATV — ad suppression extension.
 *
 * Entry points called from patched bytecode:
 *
 *   skipAllMedia3AdGroups — transforms the media3 AdPlaybackState map
 *   skipAllExo2AdGroups   — same for the ExoPlayer2 / GMS Ads variant
 *   isAdSegmentUrl        — checks if a URL is a known ad CDN segment
 *
 * --- AdPlaybackState suppression (Hooks 1 & 2) ---
 *
 * withRemovedAdGroupCount(adGroupCount) physically removes all ad groups
 * from the AdPlaybackState before ExoPlayer sees the map. Operates at
 * the ad schedule layer — suppresses mid-roll ad state machine.
 *
 * --- Segment delivery filter (Hook 3) ---
 *
 * isAdSegmentUrl() is called at index 0 of DefaultHttpDataSource.open().
 * Checks the URL being fetched against PCAP-confirmed ad CDN patterns.
 * Returns true for ad segment URLs — the caller then returns 0L (empty
 * segment) before any network connection is established.
 *
 * This operates at the segment delivery layer which fires BEFORE the WASM
 * runtime pre-buffers ad content — enabling pre-roll suppression that
 * setAdPlaybackStates alone cannot achieve due to timing constraints.
 *
 * Ad URL patterns (PCAP-validated from Onn 4K TV session analysis):
 *   avoddashs3ww      — Akamai ad video CDN
 *   aivottevtad       — Akamai ad event tracking CDN
 *   vod-dash-pv-ta    — Amazon Akamai CDN (pv-ta = Prime Video Targeted Ads)
 *   ters-             — SGAI ad stitching servers (ters-sgai1, ters-draper1 etc)
 *   aiv-delivery.net  — combined with ters- to avoid blocking content manifest
 *   vod-dash.main.amazon.pv-cdn.net    — Amazon PV-CDN ad segments
 *   pop-vod-dash.main.amazon.pv-cdn.net — Amazon PV-CDN pop variant
 *   emt-cf.live.pv-cdn.net             — Amazon live stream ad CDN
 *
 * Explicitly excluded (content delivery):
 *   cf-trickplay      — scrubber thumbnail CDN (safe harbor)
 *   cf-timedtext      — subtitle/caption CDN (safe harbor)
 *   api.us-east-1     — content manifest API (safe harbor)
 *
 * All methods wrapped in try/catch — any failure returns safe defaults
 * so playback degrades gracefully.
 */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class SkipAdsPatch {

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
            for (Object o : adPlaybackStates.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                Object key = entry.getKey();
                androidx.media3.common.AdPlaybackState state =
                        (androidx.media3.common.AdPlaybackState) entry.getValue();
                if (state.adGroupCount > state.removedAdGroupCount) {
                    state = state.withRemovedAdGroupCount(state.adGroupCount);
                }
                builder.put(key, state);
            }
            return builder.build();
        } catch (Exception e) {
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
            for (Object o : adPlaybackStates.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                Object key = entry.getKey();
                com.google.android.exoplayer2.source.ads.AdPlaybackState state =
                        (com.google.android.exoplayer2.source.ads.AdPlaybackState) entry.getValue();
                if (state.adGroupCount > state.removedAdGroupCount) {
                    state = state.withRemovedAdGroupCount(state.adGroupCount);
                }
                builder.put(key, state);
            }
            return builder.build();
        } catch (Exception e) {
            return adPlaybackStates;
        }
    }

    // ── Segment delivery filter ────────────────────────────────────────────────

    /**
     * Returns true if the given URL is a known ad segment CDN endpoint.
     *
     * Called at index 0 of DefaultHttpDataSource.open(DataSpec).
     * When true, the caller returns 0L (empty segment) immediately,
     * preventing any network connection to the ad CDN.
     *
     * URL patterns are derived from PCAPdroid captures on Onn 4K TV
     * running Prime Video ATV v6.23.23 during pre-roll and mid-roll
     * ad sessions. Content delivery domains are explicitly excluded.
     *
     * @param url The full URL string from DataSpec.uri.toString()
     * @return true if the URL is an ad segment that should be blocked
     */
    public static boolean isAdSegmentUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            // Akamai ad CDN variants (PCAP-confirmed)
            if (url.contains("avoddashs3ww")) return true;
            if (url.contains("aivottevtad")) return true;

            // Amazon Akamaized ad CDN — "pv-ta" = Prime Video Targeted Ads
            if (url.contains("vod-dash-pv-ta-amazon")) return true;

            // TERS SGAI ad stitching servers — wildcard catches all rotations
            // (ters-sgai1, ters-draper1, etc.)
            // Explicitly exclude api.us-east-1.aiv-delivery.net (content manifest)
            if (url.contains("ters-") && url.contains("aiv-delivery.net")) return true;

            // Amazon PV-CDN ad segment delivery
            // Explicitly exclude cf-trickplay and cf-timedtext (content delivery)
            if (url.contains("pv-cdn.net")) {
                if (url.contains("cf-trickplay")) return false;
                if (url.contains("cf-timedtext")) return false;
                if (url.contains("vod-dash.main.amazon")) return true;
                if (url.contains("pop-vod-dash.main.amazon")) return true;
                if (url.contains("emt-cf.live")) return true;
                if (url.contains("emt-fastly.live")) return true;
                // Known ad prefix patterns from PCAP
                if (url.contains("abgqa65")) return true;
                if (url.contains("abepmbf")) return true;
                if (url.contains("abgarbv")) return true;
            }

            return false;
        } catch (Exception e) {
            // Safe default — allow the request through on any error
            return false;
        }
    }
}
