/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class NotificationsService extends Service {
    public static final String ACTION_START_BACKGROUND = "org.telegram.NotificationsService.START_BACKGROUND";
    public static final String ACTION_START_FOREGROUND = "org.telegram.NotificationsService.START_FOREGROUND";
    public static final String CHANNEL_ID = "push_service_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();
    }

    private void doForegroundThings() {
        createNotificationChannel();
        boolean showDialog = xyz.nextalone.nagram.NaConfig.INSTANCE.getPushServiceTypeInAppDialog().Bool();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setShowWhen(false)
                .setOngoing(true)
                .setSmallIcon(NotificationsController.getNotificationIconResId())
                .setContentText(LocaleController.getString("NekoXPushService", R.string.NekoXPushService))
                .setPriority(showDialog ? NotificationCompat.PRIORITY_LOW : NotificationCompat.PRIORITY_MIN);
        try {
            startForeground(9999, builder.build());
        } catch (Throwable e) {
            Log.e("TFOSS", "Failed to start push service", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            boolean showDialog = xyz.nextalone.nagram.NaConfig.INSTANCE.getPushServiceTypeInAppDialog().Bool();
            int importance = showDialog ? NotificationManager.IMPORTANCE_LOW : NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, LocaleController.getString("NekoXPushService", R.string.NekoXPushService), importance);
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        super.onTimeout(startId, fgsType);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            doForegroundThings();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        try {
            stopForeground(true);
        } catch (Throwable ignore) {
        }
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        if (preferences.getBoolean("pushService", true)) {
            Intent intent = new Intent("org.telegram.start");
            intent.setPackage(getPackageName());
            try {
                sendBroadcast(intent);
            } catch (Exception ex) {
                // 辣鷄miui 就你事最多.jpg
            }
        }
    }

}
