package ajstrick81.morphe.patches.peacock.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ── Layer 1 ──────────────────────────────────────────────────────────────────
// Target: SSAIConfiguration$MediaTailor$AutomaticMediaTailor.getProxyHost()
// Returns the MediaTailor SSAI proxy URL. Returning "" disables SSAI.
// Confirmed matching v7.5.102.
internal object MediaTailorProxyHostFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/String;",
    custom = { method, classDef ->
        method.name == "getProxyHost" &&
            classDef.type.contains("AutomaticMediaTailor")
    },
)

// ── Layer 3 ──────────────────────────────────────────────────────────────────
// Target: MediaTailorAdvertServiceFactoryImpl — method containing unique
// error string "Could not build MT Advertising service".
// Returning null aborts service construction.
// Confirmed matching v7.5.102.
internal object MediaTailorAdServiceMethodFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/lang/Object;",
    strings = listOf("Could not build MT Advertising service"),
)

// ── Layer 4 ──────────────────────────────────────────────────────────────────
// Target: Configuration.getSsaiConfigurationProvider()
// Returning null forces strategyForType() → AdvertisingStrategy.None
// for all playback types via confirmed if-eqz branch. No crash risk.
// Confirmed matching v7.5.102.
internal object SsaiConfigurationProviderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lcom/sky/core/player/sdk/addon/SSAIConfigurationProvider;",
    custom = { method, classDef ->
        method.name == "getSsaiConfigurationProvider" &&
            classDef.type == "Lcom/sky/core/player/sdk/data/Configuration;"
    },
)

// ── Layer 5 (PENDING) ─────────────────────────────────────────────────────────
// PlayerEngineItemImpl.handleAdBreakStarted() — deferred.
// "handleAdBreakStarted" only appears in Kotlin metadata class name strings,
// not as a const-string instruction in any method body. Correct anchor
// string needs to be identified from merged APK smali before re-adding.

// ── Layer 6 ──────────────────────────────────────────────────────────────────
// Target: NetworkingKt.getOkHttpClient()
// Injects AdBlockInterceptor into the OkHttp client builder before build().
// Intercepts ad CDN and analytics domains at the network layer, replacing
// the AGH DNS dependency with an in-app equivalent.
// custom alone is sufficient — NetworkingKt.getOkHttpClient is unique in APK.
// Confirmed matching v7.5.102.
internal object GetOkHttpClientFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Lokhttp3/OkHttpClient;",
    custom = { method, classDef ->
        method.name == "getOkHttpClient" &&
            classDef.type == "Lcom/peacock/peacocktv/util/NetworkingKt;"
    },
)

// ── Layer 7 ──────────────────────────────────────────────────────────────────
// Target: XTVWebView.<init>(Context)
// Injection point: instruction index 54 (bytecode offset 252), immediately
// before the setWebViewClient(xtvClient) call.
//
// PCAP/GREASE fingerprinting confirmed that ad segment delivery and all
// FreeWheel/analytics traffic travels through the Chromium network stack
// inside XTVWebView, bypassing OkHttp entirely. xtvClient (XTVWebView$xtvClient$1)
// extends WebViewClient but does NOT override shouldInterceptRequest.
//
// custom guard on class + method name + single Context parameter is sufficient —
// XTVWebView has three <init> overloads; this selects only (Context).
// No strings anchor: "WebViewClient.onLoadResource." lives in xtvClient$1,
// not in this method body, and would cause a match failure.
// Confirmed matching v7.5.102.
internal object XtvClientWrapFingerprint : Fingerprint(
    custom = { method, classDef ->
        method.name == "<init>" &&
            method.parameters.size == 1 &&
            method.parameters[0].type == "Landroid/content/Context;" &&
            classDef.type == "Lcom/peacock/peacocktv/web/XTVWebView;"
    },
)
