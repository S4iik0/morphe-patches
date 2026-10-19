package app.morphe.extension.youtube.patches;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OpenLinksExternallyPatch {

    /**
     * Browser used for links that have no dedicated app.
     * Waterfox for Android. Change this if you use another browser (ex: org.mozilla.firefox).
     */
    private static final String PREFERRED_BROWSER_PACKAGE = "net.waterfox.android.release";

    /**
     * Return the intent to open links with. If empty, the link will be opened with the default browser.
     *
     * @param originalIntent The original intent to open links with.
     * @return The intent to open links with. Empty means the link will be opened with the default browser.
     */
    public static String getIntent(String originalIntent) {
        if (Settings.EXTERNAL_BROWSER.get()) return "";

        return originalIntent;
    }

    /**
     * Open the link in a dedicated (non-browser) app if one exists (ex: GitHub app for github.com),
     * otherwise in the preferred browser.
     *
     * @return null if the link was opened (nothing more to do),
     *         otherwise the original Uri so the normal flow continues.
     */
    public static Uri openInDedicatedApp(Context context, Uri uri) {
        try {
            if (!Settings.EXTERNAL_BROWSER.get()) return uri;
            if (context == null || uri == null) return uri;

            Uri target = uri;

            // Unwrap youtube.com/redirect?q=<real link>
            String host = uri.getHost();
            if (host != null && host.endsWith("youtube.com") && "/redirect".equals(uri.getPath())) {
                String query = uri.getQueryParameter("q");
                if (query != null) target = Uri.parse(query);
            }

            String scheme = target.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return uri;
            }

            final Uri finalTarget = target;

            // 1. Dedicated app (API 30+: REQUIRE_NON_BROWSER only matches non-browser apps).
            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, finalTarget)
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER));
                    Logger.printDebug(() -> "Opened in dedicated app: " + finalTarget);
                    return null;
                } catch (ActivityNotFoundException ex) {
                    Logger.printDebug(() -> "No dedicated app for: " + finalTarget);
                } catch (Exception ex) {
                    Logger.printException(() -> "Dedicated app launch failure", ex);
                }
            }

            // 2. Preferred browser, explicitly.
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, finalTarget)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setPackage(PREFERRED_BROWSER_PACKAGE)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                Logger.printDebug(() -> "Opened in preferred browser: " + finalTarget);
                return null;
            } catch (Exception ex) {
                Logger.printDebug(() -> "Preferred browser not available: " + ex);
            }

            // 3. Let YouTube's normal flow open the default browser.
            return uri;
        } catch (Exception ex) {
            Logger.printException(() -> "openInDedicatedApp failure", ex);
            return uri;
        }
    }
}
