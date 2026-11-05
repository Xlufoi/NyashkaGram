package tw.nekomimi.nekogram.helpers;

import android.app.Application;

public class AnalyticsHelper {
    public static String DSN = "";
    public static boolean loaded = false;

    public static void start(Application application) {
        // Analytics completely purged
    }

    public static void captureException(Throwable e) {
        // Analytics completely purged
    }

    public static boolean getSentryStatus(Application application) {
        return false;
    }
}
