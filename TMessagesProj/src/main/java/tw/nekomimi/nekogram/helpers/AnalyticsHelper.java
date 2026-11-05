package tw.nekomimi.nekogram.helpers;

import android.app.Application;

import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;


import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.android.core.SentryAndroid;
import xyz.nextalone.nagram.NkmrConfig;

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
