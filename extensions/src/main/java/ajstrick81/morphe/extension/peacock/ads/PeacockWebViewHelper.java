package ajstrick81.morphe.extension.peacock.ads;

import android.util.Log;
import android.webkit.ClientCertRequest;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;

/**
 * Layer 7 — WebView shouldInterceptRequest injection.
 *
 * wrapClient() wraps the existing xtvClient and adds shouldInterceptRequest()
 * to block confirmed ad/analytics hostnames at the WebView/Chromium layer.
 *
 * Critical: onReceivedClientCertRequest MUST be delegated. Peacock uses mutual
 * TLS with a client certificate (com.peacock.peacocktv.tamper.Loader) to
 * authenticate the app to its servers. Dropping this callback silently kills
 * the TLS handshake and prevents the page from loading entirely.
 *
 * AD_HOSTS strategy — four categories:
 *   1. Ad config endpoints (vac, sas) — kill ad decision before it starts
 *   2. FreeWheel CSAI — block ad requests and NBC ad tech
 *   3. MediaTailor SSAI — belt-and-suspenders alongside Layers 1/3/4
 *   4. Analytics — suppress impression/completion reporting
 *
 * SAFE_HOSTS always pass through — Peacock's own infrastructure is never blocked.
 * Netskrt CDN shards are blocked by pattern EXCEPT -ns suffix (content delivery).
 */
public class PeacockWebViewHelper {

    private static final String TAG = "MORPHE-PCK-WV";

    private static final String[] SAFE_HOSTS = {
        "peacocktv.com",
        "nbcuni.com",
        "nbcuniversal.com",
    };

    private static final String[] AD_HOSTS = {
        // ── Ad config — kills the ad decision pipeline before FreeWheel is contacted
        "vac.peacocktv.com",
        "sas.peacocktv.com",

        // ── FreeWheel CSAI
        "fwmrm.net",
        "video-ads-module.ad-tech.nbcuni.com",

        // ── MediaTailor SSAI — belt-and-suspenders alongside Layers 1/3/4
        "mediatailor.",

        // ── Analytics / measurement
        "scorecardresearch.com",
        "imrworldwide.com",
        "omtrdc.net",
        "doubleverify.com",
        "adsafeprotected.com",
        "innovid.com",
        "agkn.com",
        "doubleclick.net",
        "rlcdn.com",
        "nbcuas.com",
    };

    // Netskrt CDN shards that serve ads — block all EXCEPT the -ns suffix
    // which serves legitimate content. Mirrors the AGH negative lookahead rule:
    // /^g\d{1,4}-[a-z0-9]+-us-cmaf-prd-(?!ns)[a-z0-9-]+\.prd\.pck\.netskrt\.net$/
    private static final String NETSKRT_DOMAIN = ".prd.pck.netskrt.net";
    private static final String NETSKRT_SAFE_SUFFIX = "-ns.prd.pck.netskrt.net";

    private static WebResourceResponse emptyResponse() {
        return new WebResourceResponse(
            "text/plain",
            "utf-8",
            new ByteArrayInputStream(new byte[0])
        );
    }

    private static boolean shouldBlock(String url) {
        // Safe-list — Peacock's own infrastructure always passes through
        // Note: vac.peacocktv.com and sas.peacocktv.com are subdomains of
        // peacocktv.com, so we check AD_HOSTS BEFORE the safe-list for those.
        // We handle this by checking AD_HOSTS first, then safe-list.

        // Ad config endpoints — check before safe-list since they're
        // subdomains of peacocktv.com but we want to block them
        if (url.contains("vac.peacocktv.com") || url.contains("sas.peacocktv.com")) {
            return true;
        }

        // Safe-list check
        for (String safe : SAFE_HOSTS) {
            if (url.contains(safe)) return false;
        }

        // Netskrt CDN — block all shards except -ns (content)
        if (url.contains(NETSKRT_DOMAIN) && !url.contains(NETSKRT_SAFE_SUFFIX)) {
            return true;
        }

        // Standard ad host blocklist
        for (String ad : AD_HOSTS) {
            if (url.contains(ad)) return true;
        }

        return false;
    }

    public static WebViewClient wrapClient(final WebViewClient original) {
        Log.d(TAG, "PeacockWebViewHelper.wrapClient() — Layer 7 active");
        return new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                try {
                    String url = request.getUrl().toString();
                    if (shouldBlock(url)) {
                        Log.d(TAG, "BLOCKED: " + url);
                        return emptyResponse();
                    }
                    return null;
                } catch (Exception e) {
                    Log.e(TAG, "shouldInterceptRequest error: " + e.getMessage());
                    return null;
                }
            }

            // ── Critical: mutual TLS client certificate ───────────────────
            @Override
            public void onReceivedClientCertRequest(WebView view,
                    ClientCertRequest certRequest) {
                original.onReceivedClientCertRequest(view, certRequest);
            }

            // ── Delegate all xtvClient overrides ─────────────────────────
            @Override
            public void onPageStarted(WebView view, String url,
                    android.graphics.Bitmap favicon) {
                original.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                original.onPageFinished(view, url);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                original.onLoadResource(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {
                original.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onReceivedHttpError(WebView view,
                    WebResourceRequest request,
                    WebResourceResponse errorResponse) {
                original.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onReceivedSslError(WebView view,
                    android.webkit.SslErrorHandler handler,
                    android.net.http.SslError error) {
                original.onReceivedSslError(view, handler, error);
            }

            @Override
            public boolean onRenderProcessGone(WebView view,
                    android.webkit.RenderProcessGoneDetail detail) {
                return original.onRenderProcessGone(view, detail);
            }
        };
    }
}
