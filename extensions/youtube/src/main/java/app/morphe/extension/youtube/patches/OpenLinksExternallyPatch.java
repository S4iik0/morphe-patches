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
     * Try to open the link in a dedicated (non-browser) app, for example the GitHub app for github.com links.
     * The intent is built as a plain ACTION_VIEW without a MIME type, so Android app link handling applies.
     *
     * @return null if a dedicated app was launched (nothing more to do),
     *         otherwise the original Uri so the normal flow continues (opens the browser).
     */
    public static Uri openInDedicatedApp(Context context, Uri uri) {
        try {
            if (!Settings.EXTERNAL_BROWSER.get()) return uri;
            if (context == null || uri == null) return uri;
            if (Build.VERSION.SDK_INT < 30) return uri; // FLAG_ACTIVITY_REQUIRE_NON_BROWSER needs API 30.

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

            Intent intent = new Intent(Intent.ACTION_VIEW, target)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER);
            context.startActivity(intent);

            Logger.printDebug(() -> "Opened link in dedicated app: " + target);
            return null;
        } catch (ActivityNotFoundException ex) {
            // Only browsers can handle this link: let the normal flow open the browser.
            return uri;
        } catch (Exception ex) {
            Logger.printException(() -> "openInDedicatedApp failure", ex);
            return uri;
        }
    }
}
