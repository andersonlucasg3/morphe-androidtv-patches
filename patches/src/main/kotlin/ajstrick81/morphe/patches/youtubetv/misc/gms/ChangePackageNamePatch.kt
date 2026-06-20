package ajstrick81.morphe.patches.youtubetv.misc.gms

import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.youtubetv.shared.Constants

/**
 * Manifest rewriting patch for YouTube Android TV package rename.
 *
 * Changes the package name from com.google.android.youtube.tv to
 * app.morphe.android.youtube.tv for MicroG support and side-by-side
 * installation alongside the official YouTube TV app.
 *
 * Ported from Morphe mobile: all/misc/packagename/ChangePackageNamePatch.kt
 *
 * WHY THIS MATTERS:
 * - Android identifies apps by package name. If MicroG's reimplementation
 *   of GMS uses the same package name as the original, it must exactly
 *   match the package the app expects. We rename the app's package so
 *   MicroG (at app.revanced.android.gms) can intercept without conflict.
 *
 * - Side-by-side installation: the renamed package allows users to keep
 *   the official YouTube TV app installed alongside the Morphe-patched
 *   version for testing and fallback purposes.
 *
 * - Content providers, broadcast receivers, and GCM permissions in the
 *   manifest all reference the package name and must be rewritten.
 *
 * STATUS: SCAFFOLD — manifest transformations require Phase 0 RE data
 * (exact content provider authorities, permission strings, and intent
 * filters) and the Morphe v1.3.0 resource patching API must be verified.
 *
 * CURRENT MORPHE v1.3.0 API SURFACE CHECK:
 * This repo does not contain ResourcePatch imports or transformStringReferences
 * calls in any existing patch files. The following APIs must be verified
 * against the actual morphe-patcher v1.3.0 dependency:
 *
 *   - app.morphe.patcher.patch.ResourcePatch       (base class for resource patches)
 *   - app.morphe.patcher.extensions.ResourceExtensions  (manifest/doc manipulation)
 *   - AndroidManifest.xml manipulation API          (document model)
 *
 * If none of these are available, the manifest rewriting may need to be
 * implemented as a DocumentPatch or as a build-time preprocessing step
 * (e.g., apktool decode + sed + apktool build).
 *
 * Reference implementation:
 *   MorpheApp/morphe-patches:
 *     all/misc/packagename/ChangePackageNamePatch.kt
 *     shared/misc/gms/GmsCoreSupportPatch.kt (gmsCoreSupportResourcePatch companion)
 */
@Suppress("unused")
val changePackageNamePatch = bytecodePatch(
    name = "Change package name",
    description = "Changes package name from com.google.android.youtube.tv " +
        "to app.morphe.android.youtube.tv for MicroG support.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        // ═════════════════════════════════════════════════════════════════════
        // CHANGE PACKAGE NAME — MANIFEST TRANSFORMATIONS
        //
        // These transformations operate on AndroidManifest.xml at the
        // document level. Each transformation replaces the original
        // Google package identifiers with the Morphe equivalents.
        //
        // The transformations are ORDERED from most-specific to
        // most-general to prevent partial replacements from corrupting
        // longer strings.
        // ═════════════════════════════════════════════════════════════════════

        // ─────────────────────────────────────────────────────────────────────
        // Transformation 1 — RENAME PACKAGE
        //
        // The root <manifest> element's package attribute:
        //   package="com.google.android.youtube.tv"
        //   → package="app.morphe.android.youtube.tv"
        //
        // This is the PRIMARY rename. All relative class names in the
        // manifest (Activity, Service, Receiver, Provider android:name
        // starting with ".") resolve against this package.
        //
        // API (if available):
        //   manifestDocument.setPackageName("app.morphe.android.youtube.tv")
        // or:
        //   manifestNode.attributes["package"] = "app.morphe.android.youtube.tv"
        // ─────────────────────────────────────────────────────────────────────
        // TODO Phase 0 + API verification: Implement package rename.
        // After renaming, verify:
        //   - All relative class names still resolve correctly
        //   - No absolute class names contain the old package
        //   - R.class / BuildConfig references are updated

        // ─────────────────────────────────────────────────────────────────────
        // Transformation 2 — REWRITE CONTENT PROVIDER AUTHORITIES
        //
        // All <provider> elements with android:authorities containing the
        // original package name must be rewritten to the new package name.
        //
        // Expected providers (to be confirmed in Phase 0):
        //   - FileProvider:
        //       android:authorities="com.google.android.youtube.tv.fileprovider"
        //       → android:authorities="app.morphe.android.youtube.tv.fileprovider"
        //
        //   - SuggestionProvider (search suggestions):
        //       android:authorities="com.google.android.youtube.tv.SuggestionProvider"
        //       → android:authorities="app.morphe.android.youtube.tv.SuggestionProvider"
        //
        //   - InitProvider / StartupProvider (content init):
        //       android:authorities="com.google.android.youtube.tv.init"
        //       → android:authorities="app.morphe.android.youtube.tv.init"
        //
        //   - Any other app-specific content providers discovered via
        //     Phase 0 APK decompilation (check the full <application>
        //     element of the decoded manifest).
        //
        // API (if available):
        //   For each <provider> node in manifest:
        //     providerNode.attributes["android:authorities"] =
        //         providerNode.attributes["android:authorities"]
        //             .replace("com.google.android.youtube.tv",
        //                      "app.morphe.android.youtube.tv")
        // ─────────────────────────────────────────────────────────────────────
        // TODO Phase 0: Extract decoded AndroidManifest.xml from the YouTube
        // TV APK and catalog ALL content provider authorities containing
        // the original package name.

        // ─────────────────────────────────────────────────────────────────────
        // Transformation 3 — REWRITE GCM/C2DM PERMISSIONS
        //
        // GCM-related permission attributes in the manifest:
        //
        //   <permission>
        //       android:name="com.google.android.c2dm.permission.SEND"
        //       → android:name="app.revanced.android.c2dm.permission.SEND"
        //   </permission>
        //
        //   <uses-permission>
        //       android:name="com.google.android.c2dm.permission.RECEIVE"
        //       → android:name="app.revanced.android.c2dm.permission.RECEIVE"
        //   </uses-permission>
        //
        //   android:permission="com.google.android.c2dm.permission.SEND"
        //   → android:permission="app.revanced.android.c2dm.permission.SEND"
        //   (as attribute on <service>, <receiver>, or <activity> elements)
        //
        // Also rename:
        //   - com.google.android.gms.permission.* → app.revanced.android.gms.permission.*
        //   - com.google.android.googleapps.permission.* → app.revanced.android.googleapps.permission.*
        // ─────────────────────────────────────────────────────────────────────
        // TODO Phase 0 + API verification: Rewrite all GCM/GMS permission
        // references in the manifest using the available resource patching API.

        // ─────────────────────────────────────────────────────────────────────
        // Transformation 4 — ADD <queries> ENTRY FOR MICROG
        //
        // Android 11+ (API 30+) requires apps to declare which packages
        // they want to query in a <queries> element. To interact with
        // MicroG (app.revanced.android.gms), we must add:
        //
        //   <queries>
        //       <package android:name="app.revanced.android.gms"/>
        //   </queries>
        //
        // This enables:
        //   - PackageManager queries for MicroG availability
        //   - Service bindings to MicroG's GMS implementation
        //   - Content provider resolution for MicroG providers
        //
        // If the manifest already has a <queries> element, append to it.
        // If it doesn't, create it as a child of <manifest>.
        // ─────────────────────────────────────────────────────────────────────
        // TODO Phase 0 + API verification: Inject <queries> element if the
        // API supports document-level manifest manipulation.

        // ─────────────────────────────────────────────────────────────────────
        // Transformation 5 — ADD SPOOFED METADATA TO <application>
        //
        // MicroG uses spoofed metadata to convince the patched app that
        // it is running alongside genuine Google Play Services. These
        // metadata entries are injected into the <application> element:
        //
        //   <meta-data
        //       android:name="app.revanced.android.gms.SPOOFED_PACKAGE_NAME"
        //       android:value="com.google.android.youtube.tv"/>
        //
        //   <meta-data
        //       android:name="app.revanced.android.gms.SPOOFED_PACKAGE_SIGNATURE"
        //       android:value="<SHA-1 fingerprint extracted during Phase 0>"/>
        //
        //   <meta-data
        //       android:name="app.revanced.MICROG_PACKAGE_NAME"
        //       android:value="app.revanced.android.gms"/>
        //
        // The SPOOFED_PACKAGE_SIGNATURE must be the actual SHA-1
        // certificate fingerprint of the original YouTube TV APK,
        // extracted during Phase 0 RE. This signature is verified
        // by MicroG during the spoofed GMS handshake.
        //
        // Extraction method: keytool -printcert -file META-INF/CERT.RSA
        // from the original APK, or via PackageManager at runtime.
        // ─────────────────────────────────────────────────────────────────────
        // TODO Phase 0: Extract SHA-1 certificate fingerprint from the
        // original YouTube TV APK. This is REQUIRED for MicroG signature
        // spoofing to function correctly.

        // ═════════════════════════════════════════════════════════════════════
        // IMPLEMENTATION STATUS: SCAFFOLD COMPLETE
        //
        // All 5 manifest transformations are documented with their purpose,
        // expected behavior, and reference implementations.
        //
        // NEXT STEPS (gated on Phase 0 RE and API verification):
        //
        // 1. Decompile YouTube TV APK and extract AndroidManifest.xml
        // 2. Catalog all content provider authorities
        // 3. Extract SHA-1 certificate fingerprint
        // 4. Verify Morphe v1.3.0 resource patching API:
        //    - Does ResourcePatch exist?
        //    - Is there a document-level manifest API?
        //    - Can <meta-data> elements be injected?
        //    - Can <queries> elements be added/appended?
        //
        // 5. If resource patching API IS available:
        //    - Convert this bytecodePatch to a ResourcePatch subclass
        //    - Implement all 5 transformations using the document API
        //    - Add before/after assertions in tests
        //
        // 6. If resource patching API is NOT available:
        //    - Implement as a build-time preprocessing step
        //    - apktool decode → modify manifest → apktool build
        //    - Or use a manifest merger/hook in the Gradle plugin
        //
        // 7. Add a companion resource patch (gmsCoreSupportResourcePatch)
        //    following the pattern in:
        //    MorpheApp/morphe-patches:
        //      shared/misc/gms/GmsCoreSupportPatch.kt -> gmsCoreSupportResourcePatch
        // ═════════════════════════════════════════════════════════════════════
    }
}
