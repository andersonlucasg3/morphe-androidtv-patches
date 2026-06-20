package ajstrick81.morphe.patches.youtubetv.misc.gms

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * GMS-dependent method fingerprints for YouTube Android TV.
 *
 * ALL FINGERPRINTS REQUIRE PHASE 0 REVERSE ENGINEERING.
 * The string values and class names below are placeholders copied from
 * the mobile YouTube patches (MorpheApp/morphe-patches) and must be
 * verified against the actual com.google.android.youtube.tv APK.
 *
 * Ported from Morphe mobile: youtube/misc/gms/Fingerprints.kt
 *
 * USAGE:
 * - Fingerprints 1-4 are consumed by GmsCoreSupportPatch for string replacement
 * - Fingerprint 5 (CastContextFetch) is a safe no-op if absent on TV
 */

// ═════════════════════════════════════════════════════════════════════════════
// Fingerprint 1 — ServiceCheckFingerprint
//
// Finds the method that checks "Google Play Services not available".
// In mobile YouTube, this is in the application bootstrap sequence
// (Shell_Application or similar class) and runs during cold-start to
// verify GMS availability before any UI is shown.
//
// Action: returnEarly() — short-circuit the GMS availability check entirely.
// When hooking with CheckEnvironmentPatch, this ensures the app never
// aborts due to a missing Play Services check, since MicroG will be
// loaded before this code path executes.
// ═════════════════════════════════════════════════════════════════════════════
object ServiceCheckFingerprint : Fingerprint(
    strings = listOf("Google Play Services not available"),
    custom = { method, classDef ->
        // TODO Phase 0: verify exact defining class for YouTube TV.
        // In mobile, this is in Shell_Application or a similar
        // application bootstrap class. The TV variant may use a
        // different class (e.g., LeanbackApplication).
        method.returnType == "V" &&
            (classDef.type.contains("Application") ||
             classDef.type.contains("Shell") ||
             classDef.type.contains("Bootstrap"))
    },
)

// ═════════════════════════════════════════════════════════════════════════════
// Fingerprint 2 — GooglePlayUtilityFingerprint
//
// Finds the GMS utility/version check method. In mobile YouTube, this
// matches a method containing three distinct strings:
//   - "This should never happen." (assertion string)
//   - "MetadataValueReader" (GMS metadata reader class name)
//   - "com.google.android.gms" (GMS package name)
//
// The method reads GMS metadata from the manifest and returns an integer
// status code. Returning 0 signals "GMS not needed / not found" and
// allows the app to proceed through its MicroG compatibility path.
//
// Action: returnEarly(0) — return integer 0 (GMS not available/not needed).
// This must be injected via addInstructions because returnEarly alone
// may not set the correct return register.
// ═════════════════════════════════════════════════════════════════════════════
object GooglePlayUtilityFingerprint : Fingerprint(
    strings = listOf(
        "This should never happen.",
        "MetadataValueReader",
        "com.google.android.gms"
    ),
    custom = { method, _ ->
        // TODO Phase 0: verify exact method signature for YouTube TV.
        // The TV APK may use a different GMS utility class or may
        // omit certain strings depending on how GMS is integrated.
        method.returnType == "I"
    },
)

// ═════════════════════════════════════════════════════════════════════════════
// Fingerprint 3 — PrimeMethodFingerprint
//
// Finds the method containing the original package name string
// "com.google.android.youtube.tv". This method is responsible for
// constructing or returning the app's package identity string, which
// is used throughout the app for:
//   - Content provider authority construction
//   - File path resolution (getExternalFilesDir, etc.)
//   - Package name checks against GMS services
//   - Intent target resolution
//
// Action: replace the string with "app.morphe.android.youtube.tv".
// This ensures all runtime package name references use the Morphe
// package identifier instead of the original Google package name.
//
// For YouTube TV specifically, this targets:
//   "com.google.android.youtube.tv" (NOT "com.google.android.youtube")
// ═════════════════════════════════════════════════════════════════════════════
object PrimeMethodFingerprint : Fingerprint(
    strings = listOf("com.google.android.youtube.tv"),
    custom = { method, _ ->
        // TODO Phase 0: verify this is the correct method to target for
        // package name string replacement. The method may be named
        // something like getPackageName(), getAppContext(), or similar.
        // Ensure it returns the package STRING, not a Context object.
        method.returnType == "Ljava/lang/String;" ||
            method.name.contains("package") ||
            method.name.contains("identity")
    },
)

// ═════════════════════════════════════════════════════════════════════════════
// Fingerprint 4 — MainActivityOnCreateFingerprint
//
// Finds the main activity's onCreate method. Used by CheckEnvironmentPatch
// to inject the GMS core availability check early in the app lifecycle,
// before any GMS-dependent code paths execute.
//
// In mobile YouTube: Lcom/google/android/apps/youtube/app/watchwhile/MainActivity;
// For YouTube TV: expected in a similar namespace, possibly under
//   Lcom/google/android/apps/youtube/tv/watchwhile/ or
//   Lcom/google/android/apps/youtube/tv/
//
// Injected smali (injected by CheckEnvironmentPatch):
//   invoke-static/range { p0 .. p0 },
//       Lajstrick81/morphe/extension/youtubetv/patches/GmsCoreSupportPatch;->checkGmsCore(Landroid/app/Activity;)V
//
// Action: addInstructions at first instruction after super.onCreate() to
// inject the GMS check call before any GMS-dependent initialization.
// ═════════════════════════════════════════════════════════════════════════════
object MainActivityOnCreateFingerprint : Fingerprint(
    accessFlags = AccessFlags.PUBLIC.value,
    custom = { method, classDef ->
        // TODO Phase 0: find the main activity class in the YouTube TV APK.
        // Look for classes that extend Activity or FragmentActivity and
        // contain YouTube/TV-related UI initialization.
        method.name == "onCreate" &&
            method.returnType == "V" &&
            (classDef.type.contains("MainActivity") ||
             classDef.type.contains("WatchWhileActivity") ||
             classDef.type.contains("TvActivity"))
    },
)

// ═════════════════════════════════════════════════════════════════════════════
// Fingerprint 5 — CastContextFetchFingerprint
//
// MAY BE ABSENT ON ANDROID TV.
//
// In mobile YouTube: method that fetches the Google Cast context
// (depends on GMS Cast SDK). On Android TV, the device is typically
// a cast TARGET (via the built-in Chromecast receiver), not a cast
// SENDER. Therefore, the Cast SDK client library may not be included
// in the TV APK at all.
//
// If present: the method makes a call to CastContext.getSharedInstance()
// which requires a valid GMS Cast service binding.
//
// Action if matched: returnEarly() — short-circuit to prevent
// CastContext initialization failure from cascading.
//
// If absent: this fingerprint simply will not match (safe no-op).
// The Morphe framework silently skips unmatched fingerprints.
// ═════════════════════════════════════════════════════════════════════════════
object CastContextFetchFingerprint : Fingerprint(
    strings = listOf("CastContext", "CastDevice"),
    custom = { method, _ ->
        // TODO Phase 0: verify existence in YouTube TV APK.
        // Check if com.google.android.gms.cast.CastContext or
        // com.google.android.gms.cast.framework.CastContext is
        // referenced anywhere in the DEX. If not, this fingerprint
        // is safe to leave as-is (it will never match).
        //
        // If CastContext IS present (e.g., for Cast Connect or
        // remote control features), this fingerprint will need
        // a more specific class definition filter.
        method.returnType == "V" &&
            (method.name.contains("cast") || method.name.contains("Cast"))
    },
)
