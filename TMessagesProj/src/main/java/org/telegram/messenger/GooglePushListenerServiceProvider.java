package org.telegram.messenger;

import android.os.SystemClock;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.PushListenerController;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;

public class GooglePushListenerServiceProvider implements PushListenerController.IPushListenerServiceProvider {

    private Boolean hasServices;

    public GooglePushListenerServiceProvider() {}

    @Override
    public String getLogTitle() {
        return "Google Play Services";
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
                FirebaseApp.initializeApp(ApplicationLoader.applicationContext);
                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            SharedConfig.pushStringGetTimeEnd = SystemClock.elapsedRealtime();
                            if (!task.isSuccessful()) {
                                if (BuildVars.LOGS_ENABLED) {
                                    FileLog.d("Failed to get regid");
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
                            if (!TextUtils.isEmpty(token)) {
                                PushListenerController.sendRegistrationToServer(getPushType(), token);
                            }
                        });
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

    @Override
    public boolean hasServices() {
        if (hasServices == null) {
            try {
                int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ApplicationLoader.applicationContext);
                hasServices = resultCode == ConnectionResult.SUCCESS;
            } catch (Exception e) {
                FileLog.e(e);
                hasServices = false;
            }
        }
        return hasServices;
    }
}
