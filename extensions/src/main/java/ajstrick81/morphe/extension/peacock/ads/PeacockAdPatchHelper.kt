package ajstrick81.morphe.extension.peacock.ads

import okhttp3.OkHttpClient

/**
 * Static wrapper invoked from smali to inject AdBlockInterceptor into the
 * OkHttp client builder without any smali register manipulation.
 *
 * Pattern: smali passes the Builder instance (v0) directly to injectAdBlocker()
 * via invoke-static {v0}, ...->injectAdBlocker(...)V — no v-register reads,
 * writes, or move-result-object needed. This avoids the VerifyError caused by
 * type-undefined registers when addInstructions() inserts mid-method.
 *
 * Injection point in smali (SkipAdsPatch.kt Layer 6):
 *   invoke-static {v0}, Lajstrick81/morphe/extension/peacock/ads/PeacockAdPatchHelper;
 *       ->injectAdBlocker(Lokhttp3/OkHttpClient$Builder;)V
 *
 * Inserted at offset 5 of NetworkingKt.getOkHttpClient() — after Builder.<init>,
 * before the existing OkHttpWorkaroundInterceptor new-instance — so both
 * interceptors are chained with AdBlockInterceptor running first.
 */
object PeacockAdPatchHelper {

    @JvmStatic
    fun injectAdBlocker(builder: OkHttpClient.Builder) {
        builder.addInterceptor(AdBlockInterceptor())
    }
}
