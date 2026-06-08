package ajstrick81.morphe.extension.primevideo.gmb;

/**
 * GMB Diagnostic Extension — temporary, not for production use.
 *
 * Logs every GMB message event type and payload to Android logcat
 * under the tag "GMB_DIAGNOSTIC" using standard android.util.Log.d()
 * which surfaces in ADB logcat — unlike Amazon's internal logger.
 *
 * This is a pure diagnostic tool. It does not suppress any messages.
 * Every GMB message passes through normally after being logged.
 *
 * Filter ADB logcat with:
 *   adb logcat --pid=<PID> -v time | findstr "GMB_DIAGNOSTIC"
 *
 * During an ad break or cart overlay you will see entries like:
 *   GMB_DIAGNOSTIC: [TYPE] billing.reportPurchaseLaunchState
 *   GMB_DIAGNOSTIC: [PAYLOAD] {"state":"LAUNCHED",...}
 *
 * Collect all event type strings that appear during:
 *   - Pre-roll ad playback
 *   - Mid-roll ad playback
 *   - Cart/purchase overlay appearance
 *   - "Get the App" overlay appearance
 *   - FF lock activation ("fast forward unavailable")
 *
 * These strings are the targets for the production suppression patch.
 */
@SuppressWarnings("unused")
public class GMBDiagnostic {

    private static final String TAG = "GMB_DIAGNOSTIC";

    /**
     * Called at index 0 of GMBMessageProcessor.processMessage().
     * Logs the event type and payload to Android logcat.
     *
     * @param eventType The GMB event type string (e.g. "billing.reportPurchaseEvent")
     * @param payload   The JSON payload for this event
     */
    public static void logGMBMessage(String eventType, String payload) {
        try {
            android.util.Log.d(TAG, "[TYPE] " + eventType);
            // Log payload separately — payloads can be large JSON strings
            // Truncate to 500 chars to keep logcat readable
            if (payload != null && payload.length() > 0) {
                String truncated = payload.length() > 500
                    ? payload.substring(0, 500) + "...[TRUNCATED]"
                    : payload;
                android.util.Log.d(TAG, "[PAYLOAD] " + truncated);
            }
        } catch (Exception e) {
            // Silent fail — never interfere with original processMessage flow
        }
    }
}
