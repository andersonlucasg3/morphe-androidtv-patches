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

// ── Layer 5 ──────────────────────────────────────────────────────────────────
// Target: PlayerEngineItemImpl.handleAdBreakStarted(AdBreakStartedEvent)
//
// No string anchor exists in this method body — confirmed via direct dex
// disassembly that the entire method is a one-liner that constructs a
// synthetic continuation (PlayerEngineItemImpl$handleAdBreakStarted$1) and
// launches it via kotlinx.coroutines.BuildersKt.e(...). The real work lives
// in that synthetic class's invokeSuspend, which has no stable anchor of
// its own (Kotlin-compiler-generated name, could shift between builds).
//
// Fingerprinted structurally instead of by string: exact defining class +
// method name + single parameter of the uniquely-named type
// Lcom/comcast/helio/subscription/AdBreakStartedEvent; — this type only
// appears in this one method signature in the entire APK, making the
// combination of class + name + parameter type as reliable as a string
// anchor would be, without depending on synthetic naming.
// Confirmed matching v7.5.102 (private final, returns void).
internal object HandleAdBreakStartedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Lcom/comcast/helio/subscription/AdBreakStartedEvent;"),
    custom = { method, classDef ->
        method.name == "handleAdBreakStarted" &&
            classDef.type == "Lcom/sky/core/player/sdk/playerEngine/playerBase/PlayerEngineItemImpl;"
    },
)

// ── Layer 6 ──────────────────────────────────────────────────────────────────
// Target: NetworkingKt.getOkHttpClient()
// Replaces method body entirely via PeacockAdPatchHelper.buildOkHttpClient().
// AdBlockInterceptor handles OkHttp-reachable ad/analytics traffic.
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
// Injection point: instruction index 56 (bytecode offset 276), immediately
// before the setWebViewClient(xtvClient) call.
//
// PCAP/GREASE fingerprinting confirmed all ad segment delivery and FreeWheel
// traffic travels through the Chromium/WebView stack, bypassing OkHttp.
// xtvClient (XTVWebView$xtvClient$1) extends WebViewClient but does NOT
// override shouldInterceptRequest.
//
// PeacockWebViewHelper.wrapClient() delegates all existing xtvClient callbacks
// and adds shouldInterceptRequest() with randomized responses to avoid
// FreeWheel fraud detection fingerprinting.
// Confirmed matching v7.5.102.
internal object XtvClientWrapFingerprint : Fingerprint(
    custom = { method, classDef ->
        method.name == "<init>" &&
            method.parameters.size == 1 &&
            method.parameters[0].type == "Landroid/content/Context;" &&
            classDef.type == "Lcom/peacock/peacocktv/web/XTVWebView;"
    },
)

// ── Layer 8 ──────────────────────────────────────────────────────────────────
// Target: AddonInjectorImpl.di$lambda$0(AddonInjectorImpl, DI$MainBuilder)
//
// This is the Sky SDK dependency injection wiring method that imports all
// addon modules into the player's DI container. At instruction indices 16-17:
//
//   idx=16: iget-object v0, v4, AddonInjectorImpl->freewheelModule DI$Module
//   idx=17: invoke-static v5,v0,v1,v2,v3, DI$Builder$DefaultImpls->import$default(...)
//
// Skipping these two instructions prevents FreeWheel from ever being
// registered in the DI container. The player has no FreeWheel addon —
// no ad requests, no VMAP fetches, no tracking pixels. This is the Sky SDK
// equivalent of returning null from getSsaiConfigurationProvider() (Layer 4)
// but targeting CSAI/FreeWheel rather than SSAI/MediaTailor.
//
// Anchor: "FreewheelModule" is unique across the entire APK and sits at
// instruction 92 in AddonInjectorImpl.<init>, same class as di$lambda$0.
// customFingerprint guards on both method name and defining class.
// Confirmed matching v7.5.102.
internal object FreewheelModuleSkipFingerprint : Fingerprint(
    custom = { method, classDef ->
        method.name == "di\$lambda\$0" &&
            classDef.type == "Lcom/sky/core/player/sdk/addon/di/AddonInjectorImpl;"
    },
)
