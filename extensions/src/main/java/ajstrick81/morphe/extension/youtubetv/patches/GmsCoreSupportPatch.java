package ajstrick81.morphe.extension.youtubetv.patches;

import java.lang.reflect.Method;

/**
 * Runtime GMS core availability check for YouTube Android TV.
 *
 * Ported from Morphe mobile:
 *   extensions/shared-youtube/library/.../GmsCoreSupportPatch.java
 *
 * Called from injected smali in the main activity's onCreate() via:
 *   invoke-static/range { p0 .. p0 },
 *       Lajstrick81/morphe/extension/youtubetv/patches/GmsCoreSupportPatch;->checkGmsCore(Landroid/app/Activity;)V
 *
 * TV-SPECIFIC ADAPTATIONS:
 * - Battery optimization check REMOVED: Android TV devices do not aggressively
 *   kill background processes, and the TV Settings UI is unsuitable for the
 *   dialog-based battery optimization whitelist flow.
 * - Dialog-based UX REPLACED with Toast-based UX: Android TV supports Toasts
 *   natively, and they are navigable with a TV remote control.
 * - Root/mounted install detection KEPT: same logic as mobile.
 * - PackageManager check KEPT: verifies app.revanced.android.gms is installed.
 * - Content provider check KEPT: verifies MicroG's GSF content provider is
 *   running. Uses content://app.revanced.android.gsf.gservices/prefix
 *
 * ANDROID SDK IMPORTS:
 *   android.app.Activity, android.content.Context, android.content.pm.PackageManager,
 *   android.content.pm.PackageInfo, android.widget.Toast, and
 *   android.content.ContentProviderClient cannot be imported in Morphe extensions
 *   (the compilation environment lacks the full Android platform SDK).
 *   All Android platform types are accessed via reflection so the extension
 *   compiles against standard Java only. This follows the pattern established
 *   in ajstrick81.morphe.extension.tubi.ads.SkipAdsPatch.
 */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class GmsCoreSupportPatch {

    // Matches mobile: app.revanced.android.gms
    private static final String GMS_CORE_PACKAGE_NAME = "app.revanced.android.gms";

    // Original YouTube TV package for spoofed identity check
    private static final String ORIGINAL_PACKAGE_NAME = "com.google.android.youtube.tv";

    // Patched package name
    private static final String MORPHE_PACKAGE_NAME = "app.morphe.android.youtube.tv";

    // MicroG's GSF services content provider authority
    private static final String GMS_CORE_PROVIDER = "content://app.revanced.android.gsf.gservices/prefix";

    // URL to download MicroG RE if not installed
    private static final String MICROG_DOWNLOAD_URL = "https://github.com/WSTxda/MicroG-RE/releases/latest";

    /**
     * Called from injected smali in the main activity's onCreate().
     *
     * Checks:
     * 1. Root/mounted install detection (warns if GmsCore patch used with root install)
     * 2. MicroG RE installation (opens download URL if not installed)
     * 3. MicroG background service health (via content provider check)
     *
     * TV-specific: Battery optimization check is SKIPPED.
     *
     * @param activity The main activity instance (passed from smali via p0,
     *                 typed as Object to avoid android.app.Activity import)
     */
    public static void checkGmsCore(Object activity) {
        if (activity == null) return;

        // Check 1: Detect root/mounted install
        // If the package name is still the original AND patched strings are
        // missing, the user likely rooted/mounted the install without applying
        // the GMS patch.
        if (isPackageNameOriginal(activity) && !isPatched(activity)) {
            showToast(activity,
                "Do not include 'GmsCore support' patch with root install. " +
                "Please repatch the APK with GmsCore support enabled.");
            return;
        }

        // Check 2: Verify MicroG RE is installed
        if (!isGmsCoreInstalled(activity)) {
            showToast(activity,
                "MicroG RE is not installed. " +
                "Download it from: " + MICROG_DOWNLOAD_URL);
            return;
        }

        // Check 3: Verify MicroG background service is running
        if (!isGmsCoreServiceRunning(activity)) {
            showToast(activity,
                "MicroG RE is installed but not running. " +
                "Please launch MicroG RE and complete the initial setup. " +
                "If the issue persists, reinstall MicroG RE.");
            return;
        }

        // TV-SPECIFIC: Battery optimization check REMOVED.
        // TVs do not aggressively kill background processes, and the TV Settings
        // UI is unsuitable for the dialog-based battery optimization flow that
        // the mobile patch uses (PowerManager.isIgnoringBatteryOptimizations()
        // + Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).
        // All checks passed — GmsCore is ready.
    }

    // ── Package name checks ─────────────────────────────────────────────────

    /**
     * Checks if the package name has been changed from the original.
     * Uses reflection to call Activity.getPackageName().
     */
    private static boolean isPackageNameOriginal(Object activity) {
        try {
            Method getPackageName = activity.getClass().getMethod("getPackageName");
            Object packageName = getPackageName.invoke(activity);
            return ORIGINAL_PACKAGE_NAME.equals(packageName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the patched resource strings are present.
     * In mobile: checks for specific resource identifiers.
     * For TV: simplified check using package name — if it matches the Morphe
     * package name, the APK has been patched.
     */
    private static boolean isPatched(Object activity) {
        try {
            Method getPackageName = activity.getClass().getMethod("getPackageName");
            Object packageName = getPackageName.invoke(activity);
            return MORPHE_PACKAGE_NAME.equals(packageName);
        } catch (Exception e) {
            return false;
        }
    }

    // ── MicroG installation check ───────────────────────────────────────────

    /**
     * Verifies that MicroG RE (app.revanced.android.gms) is installed.
     *
     * Uses reflection to obtain the PackageManager and query for the GmsCore
     * package, since android.content.pm.PackageManager cannot be imported
     * in the Morphe extension compilation environment.
     */
    private static boolean isGmsCoreInstalled(Object activity) {
        try {
            // Activity.getPackageManager() → PackageManager
            Method getPackageManager = activity.getClass().getMethod("getPackageManager");
            Object pm = getPackageManager.invoke(activity);
            if (pm == null) return false;

            // PackageManager.getPackageInfo(String, int) → PackageInfo
            Method getPackageInfo = pm.getClass().getMethod(
                "getPackageInfo", String.class, int.class
            );
            Object info = getPackageInfo.invoke(pm, GMS_CORE_PACKAGE_NAME, 0);
            return info != null;
        } catch (Exception e) {
            // NameNotFoundException or any reflection failure → not installed
            return false;
        }
    }

    // ── MicroG background service check ─────────────────────────────────────

    /**
     * Verifies that MicroG's GSF content provider is running.
     *
     * Uses reflection to acquire a ContentProviderClient from the
     * ContentResolver. If the client is null, MicroG is not running in the
     * background. This is the same check used in the mobile GmsCoreSupportPatch.
     *
     * Reflectively calls:
     *   1. Activity.getContentResolver() → ContentResolver
     *   2. ContentResolver.acquireContentProviderClient(Uri) → ContentProviderClient
     *   3. ContentProviderClient.close()
     *
     * The content URI uses the string form to avoid importing android.net.Uri.
     * ContentResolver.acquireContentProviderClient(String) is available on
     * API 24+ (Android TV 7.0+), which covers all supported YouTube TV devices.
     */
    private static boolean isGmsCoreServiceRunning(Object activity) {
        try {
            // Activity.getContentResolver() → ContentResolver
            Method getContentResolver = activity.getClass().getMethod("getContentResolver");
            Object resolver = getContentResolver.invoke(activity);
            if (resolver == null) return false;

            // ContentResolver.acquireContentProviderClient(String authority) → ContentProviderClient
            // Uses the String overload (API 24+) to avoid importing android.net.Uri
            Method acquireClient = resolver.getClass().getMethod(
                "acquireContentProviderClient", String.class
            );
            Object client = acquireClient.invoke(resolver, GMS_CORE_PROVIDER);

            if (client != null) {
                // ContentProviderClient.close()
                Method close = client.getClass().getMethod("close");
                close.invoke(client);
                return true;
            }
            return false;
        } catch (Exception e) {
            // SecurityException (signature spoof mismatch) or any other
            // failure → MicroG is not running or not properly configured
            return false;
        }
    }

    // ── Toast display ───────────────────────────────────────────────────────

    /**
     * Shows a Toast message on Android TV using reflection.
     *
     * Toast-based UX is preferred over dialog-based UX on TV because:
     * - Toasts render natively on Android TV
     * - They don't require directional navigation (D-pad)
     * - They auto-dismiss without user interaction
     *
     * The toast is posted to the UI thread via Activity.runOnUiThread().
     *
     * Reflectively calls:
     *   1. Activity.runOnUiThread(Runnable) — to ensure Toast shows on UI thread
     *   2. Toast.makeText(Context, CharSequence, int) — to construct the Toast
     *   3. Toast.show() — to display it
     */
    private static void showToast(Object activity, String message) {
        try {
            // Post to UI thread via Activity.runOnUiThread(Runnable)
            Method runOnUiThread = activity.getClass().getMethod(
                "runOnUiThread", Runnable.class
            );

            runOnUiThread.invoke(activity, (Runnable) () -> {
                try {
                    // Toast.makeText(Context, CharSequence, int) → Toast
                    Class<?> toastClass = Class.forName("android.widget.Toast");
                    Method makeText = toastClass.getMethod(
                        "makeText",
                        Class.forName("android.content.Context"),
                        CharSequence.class,
                        int.class
                    );

                    // Toast.LENGTH_LONG = 1
                    Object toast = makeText.invoke(null, activity, message, 1);

                    // Toast.show()
                    Method show = toastClass.getMethod("show");
                    show.invoke(toast);
                } catch (Exception e) {
                    // Silent fail — Toast is non-critical UX
                }
            });
        } catch (Exception e) {
            // Silent fail — Toast is non-critical UX
        }
    }
}
