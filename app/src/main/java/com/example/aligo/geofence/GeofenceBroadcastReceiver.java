package com.example.aligo.geofence;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.aligo.MainActivity;
import com.example.aligo.R;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        GeofencingEvent event = GeofencingEvent.fromIntent(intent);

        if (event.hasError()) {
            Log.e("GEOFENCE", "ERROR: " + event.getErrorCode());
            return;
        }

        int transition = event.getGeofenceTransition();

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            String rawId = event.getTriggeringGeofences().get(0).getRequestId();
            String storeName = parseStoreName(rawId);

            Log.d("GEOFENCE", "🟢 ENTER: " + storeName);

            // 최종 문장
            String message = "AliGO에서 메모한 물건을 확인해보세요!";

            sendNotification(context, storeName, message);
            sendEventToActivity(context, storeName);
        }
    }

    // -------------------------------
    // 매장명 파싱(olive_/daiso_ 제거)
    // -------------------------------
    private String parseStoreName(String id) {

        if (id.startsWith("olive_")) {
            return "올리브영 " + id.replace("olive_", "");
        }

        if (id.startsWith("daiso_")) {
            return "다이소 " + id.replace("daiso_", "");
        }

        return id; // 혹시 예외
    }

    // -------------------------------
    // 알림 보내기
    // -------------------------------
    private void sendNotification(Context context, String storeName, String message) {

        String channelId = "geofence_channel";

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Geofence Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        // 클릭 시 앱으로 이동
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("trigger_store", storeName);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.location_red)
                .setContentTitle(storeName + " 근처입니다")
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // 알림음: 시스템 기본 사운드로 통일
        builder.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        );

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // -------------------------------
    // Activity로 전달
    // -------------------------------
    private void sendEventToActivity(Context context, String storeName) {
        Intent i = new Intent(context, MainActivity.class);
        i.putExtra("trigger_store", storeName);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(i);
    }
}
