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
 * Returns an empty 200 for all known Peacock ad delivery CDN domains and
 * analytics endpoints before any connection is made. Mirrors what AGH does
 * at the DNS level but works standalone inside the app.
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
                return emptyResponse(chain);
            }
        }

        for (final String domain : ANALYTICS_DOMAINS) {
            if (host.contains(domain)) {
                return emptyResponse(chain);
            }
        }

        return chain.proceed(chain.request());
    }

    private static Response emptyResponse(final Chain chain) {
        return new Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create(
                MediaType.parse("application/octet-stream"),
                new byte[0]))
            .build();
    }
}
