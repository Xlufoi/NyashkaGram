package org.telegram.messenger;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import xyz.nextalone.nagram.NaConfig;

public class GooglePushListenerServiceProvider implements PushListenerController.IPushListenerServiceProvider {

    private Boolean hasServices;

    public GooglePushListenerServiceProvider() {}

    public static boolean isGmsInstalled() {
        try {
            ApplicationLoader.applicationContext.getPackageManager().getPackageInfo("com.google.android.gms", 0);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isMicroGInstalled() {
        try {
            PackageManager pm = ApplicationLoader.applicationContext.getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo("com.google.android.gms", 0);
                if (pi.versionName != null && pi.versionName.toLowerCase().contains("microg")) {
                    return true;
                }
            } catch (Throwable ignored) {}
            try {
                pm.getPackageInfo("org.microg.gms", 0);
                return true;
            } catch (Throwable ignored) {}
            try {
                pm.getPackageInfo("org.microg.gms.droidguard", 0);
                return true;
            } catch (Throwable ignored) {}
            try {
                pm.getPackageInfo("com.google.android.gms", 0);
                int res = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ApplicationLoader.applicationContext);
                if (res != ConnectionResult.SUCCESS) {
                    return true;
                }
            } catch (Throwable ignored) {}
            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public String getLogTitle() {
        return NaConfig.INSTANCE.getPushServiceType().Int() == 3 || isMicroGInstalled() ? "microG Services" : "Google Play Services";
    }

    @Override
    public int getPushType() {
        return PushListenerController.PUSH_TYPE_FIREBASE;
    }

    @Override
    public void onRequestPushToken() {
        String currentPushString = SharedConfig.pushString;
        if (!TextUtils.isEmpty(currentPushString)) {
            if (BuildVars.DEBUG_PRIVATE_VERSION && BuildVars.LOGS_ENABLED) {
                FileLog.d("FCM regId = " + currentPushString);
            }
        } else {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("FCM Registration not found.");
            }
        }
        Utilities.globalQueue.postRunnable(() -> {
            try {
                SharedConfig.pushStringGetTimeStart = SystemClock.elapsedRealtime();
                FirebaseApp app = null;
                try {
                    app = FirebaseApp.getInstance();
                } catch (Throwable ignored) {}
                if (app == null) {
                    try {
                        app = FirebaseApp.initializeApp(ApplicationLoader.applicationContext);
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                }
                if (app == null) {
                    try {
                        FirebaseOptions options = new FirebaseOptions.Builder()
                                .setApplicationId("1:454087305870:android:5c8d8e9f3a9a0e37eb37df")
                                .setApiKey("AIzaSyCPNxXY201baapXdQZCpdhLYVJ8LYN1bK8")
                                .setProjectId("nekogram-2753b")
                                .setGcmSenderId("454087305870")
                                .build();
                        app = FirebaseApp.initializeApp(ApplicationLoader.applicationContext, options);
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                }
                try {
                    FirebaseMessaging.getInstance().setAutoInitEnabled(true);
                } catch (Throwable ignored) {}

                fetchTokenWithRetry(0);
            } catch (Throwable e) {
                FileLog.e(e);
                SharedConfig.pushStringStatus = "__FIREBASE_FAILED__";
                AndroidUtilities.runOnUIThread(() -> {
                    for (int a : SharedConfig.activeAccounts) {
                        org.telegram.tgnet.ConnectionsManager.getInstance(a).setPushConnectionEnabled(true);
                    }
                    ApplicationLoader.startPushService();
                });
            }
        });
    }

    private void fetchTokenWithRetry(int retryCount) {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        SharedConfig.pushStringGetTimeEnd = SystemClock.elapsedRealtime();
                        if (!task.isSuccessful() || TextUtils.isEmpty(task.getResult())) {
                            if (BuildVars.LOGS_ENABLED) {
                                FileLog.d("Failed to get FCM/microG regid (retry " + retryCount + "): " + (task.getException() != null ? task.getException().getMessage() : "null"));
                            }
                            if (retryCount < 5) {
                                int delay = (retryCount + 1) * 3000;
                                Utilities.globalQueue.postRunnable(() -> fetchTokenWithRetry(retryCount + 1), delay);
                                return;
                            }
                            SharedConfig.pushStringStatus = "__FIREBASE_FAILED__";
                            PushListenerController.sendRegistrationToServer(getPushType(), null);
                            AndroidUtilities.runOnUIThread(() -> {
                                for (int a : SharedConfig.activeAccounts) {
                                    org.telegram.tgnet.ConnectionsManager.getInstance(a).setPushConnectionEnabled(true);
                                }
                                ApplicationLoader.startPushService();
                            });
                            return;
                        }
                        String token = task.getResult();
                        if (BuildVars.LOGS_ENABLED) {
                            FileLog.d("FCM/microG token obtained successfully: " + token);
                        }
                        SharedConfig.pushStringStatus = "";
                        PushListenerController.sendRegistrationToServer(getPushType(), token);
                    });
        } catch (Throwable e) {
            FileLog.e(e);
            if (retryCount < 5) {
                int delay = (retryCount + 1) * 3000;
                Utilities.globalQueue.postRunnable(() -> fetchTokenWithRetry(retryCount + 1), delay);
                return;
            }
            SharedConfig.pushStringStatus = "__FIREBASE_FAILED__";
            AndroidUtilities.runOnUIThread(() -> {
                for (int a : SharedConfig.activeAccounts) {
                    org.telegram.tgnet.ConnectionsManager.getInstance(a).setPushConnectionEnabled(true);
                }
                ApplicationLoader.startPushService();
            });
        }
    }

    @Override
    public boolean hasServices() {
        if (hasServices == null) {
            try {
                int pushType = NaConfig.INSTANCE.getPushServiceType().Int();
                if (pushType == 3) {
                    hasServices = isGmsInstalled();
                    return hasServices;
                }
                int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ApplicationLoader.applicationContext);
                hasServices = (resultCode == ConnectionResult.SUCCESS
                        || resultCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED
                        || GoogleApiAvailability.getInstance().isUserResolvableError(resultCode));
                if (!hasServices) {
                    hasServices = isGmsInstalled();
                }
            } catch (Exception e) {
                FileLog.e(e);
                hasServices = isGmsInstalled();
            }
        }
        return hasServices;
    }
}
