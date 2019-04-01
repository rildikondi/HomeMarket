package com.akondi.homemarket.helper;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Build;

import com.akondi.homemarket.R;

public class NotificationHelper extends ContextWrapper {

    private static final String HM_CHANNEL_ID = "com.akondi.homemarket.HMID";
    private static final String HM_CHANNEL_NAME = "Home Market";

    private NotificationManager manager;

    public NotificationHelper(Context base) {
        super(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel hmChannel = new NotificationChannel(
                HM_CHANNEL_ID,
                HM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);
        hmChannel.enableLights(false);
        hmChannel.enableVibration(true);
        hmChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(hmChannel);
    }

    public NotificationManager getManager() {
        if (manager == null)
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return manager;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public android.app.Notification.Builder getHMChannelNotification(
            String title,
            String body,
            PendingIntent contentIntent,
            Uri soundUri
    ) {
        return new android.app.Notification.Builder(getApplicationContext(), HM_CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(soundUri)
                .setAutoCancel(false);
    }
}
