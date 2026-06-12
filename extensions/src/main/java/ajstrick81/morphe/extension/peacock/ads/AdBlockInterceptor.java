package ajstrick81.morphe.extension.peacock.ads;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * Peacock ATV — OkHttp ad CDN interceptor.
 *
 * Returns a 503 for all known Peacock ad delivery CDN domains and analytics
 * endpoints before any connection is made. Using 503 (not 200) ensures
 * ExoPlayer's error-handling path fires instead of the media parser, which
 * would throw an UnrecognizedInputFormatException on a 0-byte body.
 *
 * Mirrors what AGH does at the DNS level but works standalone inside the app.
 * Invoked via PeacockAdPatchHelper.injectAdBlocker() to avoid smali register
 * manipulation entirely (zero-register wrapper pattern).
 *
 * Ad CDNs (identified via AGH DNS log, live ad break capture):
 *   *-prd-fy.cdn.peacocktv.com  (Fastly)
 *   *-prd-ak.cdn.peacocktv.com  (Akamai)
 *   *-prd-cf.cdn.peacocktv.com  (Cloudflare)
 *
 * Safe CDNs — never intercepted:
 *   *-prd-mc.cdn.peacocktv.com
 *   *-prd-ns.prd.pck.netskrt.net
 *
 * Suffix matching covers future group IDs (g007, g008, etc.) automatically.
 */
@SuppressWarnings("unused")
public class AdBlockInterceptor implements Interceptor {

    private static final String[] AD_CDN_SUFFIXES = {
        "prd-fy.cdn.peacocktv.com",
        "prd-ak.cdn.peacocktv.com",
        "prd-cf.cdn.peacocktv.com",
    };

    private static final String[] ANALYTICS_DOMAINS = {
        "fwmrm.net",
        "scorecardresearch.com",
        "conviva.com",
        "imrworldwide.com",
        "omtrdc.net",
        "newrelic.com",
    };

    @Override
    public Response intercept(Chain chain) throws IOException {
        final String host = chain.request().url().host();

        for (final String suffix : AD_CDN_SUFFIXES) {
            if (host.endsWith(suffix)) {
                return blockedResponse(chain);
            }
        }

        for (final String domain : ANALYTICS_DOMAINS) {
            if (host.contains(domain)) {
                return blockedResponse(chain);
            }
        }

        return chain.proceed(chain.request());
    }

    /**
     * Returns 503 Service Unavailable.
     *
     * 503 triggers the player's internal error/skip logic rather than the
     * media parser. A 200 with a 0-byte body would cause the parser to throw
     * an UnrecognizedInputFormatException, crashing the playback loop.
     */
    private static Response blockedResponse(final Chain chain) {
        return new Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(503)
            .message("Service Unavailable")
            .body(ResponseBody.create(
                MediaType.parse("text/plain; charset=utf-8"),
                "Blocked"))
            .build();
    }
}
