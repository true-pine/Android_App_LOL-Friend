package com.capston.lolfriend.etc;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.capston.lolfriend.R;
import com.capston.lolfriend.activity.MainActivity;
import com.capston.lolfriend.activity.SplashActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyCloudMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        if (!remoteMessage.getData().isEmpty()) {
            //Read Data Payload
            String title = remoteMessage.getData().get("title").toString();
            String text = remoteMessage.getData().get("text").toString();
            String chatRoomType = remoteMessage.getData().get("chatRoomType").toString();
            String chatRoomId = remoteMessage.getData().get("chatRoomId").toString();
            String chatRoomName = remoteMessage.getData().get("chatRoomName").toString();
            //Make Notification
            sendNotification(title, text, chatRoomType, chatRoomId, chatRoomName);
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {

    }

    private void sendNotification(String title, String body, String roomType, String roomId, String roomName) {
        Intent intent;

        if (PreferenceManager.getBoolean(MyCloudMessagingService.this, "isRemindTask")) {
            intent = new Intent(MyCloudMessagingService.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("request", "notification");
            intent.putExtra("roomType", roomType);
            intent.putExtra("roomId", roomId);
            intent.putExtra("roomName", roomName);
        } else {
            intent = new Intent(MyCloudMessagingService.this, SplashActivity.class);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher_custom)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationManager.IMPORTANCE_HIGH)
                        /*.setContentIntent(pendingIntent)*/;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}
