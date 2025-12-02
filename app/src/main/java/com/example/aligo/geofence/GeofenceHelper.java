package com.example.aligo.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingRequest;

public class GeofenceHelper {

    private final Context context;

    // 반드시 하나만 유지해야 하는 PendingIntent
    private PendingIntent pendingIntent;

    public GeofenceHelper(Context context) {
        this.context = context;
    }

    // -------------------------------------------------------------------------------------
    // 지오펜스 생성
    // -------------------------------------------------------------------------------------
    public Geofence buildGeofence(String id, double lat, double lon) {

        return new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(lat, lon, 80) // 반경 80m
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setLoiteringDelay(0)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }

    // -------------------------------------------------------------------------------------
    // 지오펜싱 요청(Request)
    // -------------------------------------------------------------------------------------
    public GeofencingRequest buildRequest(Geofence geofence) {

        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    public PendingIntent getPendingIntent() {

        // 이미 만들어진 게 있으면 그걸 그대로 사용
        if (pendingIntent != null) {
            return pendingIntent;
        }

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);

        pendingIntent = PendingIntent.getBroadcast(
                context,
                1002, // requestCode (항상 동일해야 함)
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        return pendingIntent;
    }

    // -------------------------------------------------------------------------------------
    // 에러 문자열
    // -------------------------------------------------------------------------------------
    public String getErrorString(Exception e) {

        if (e instanceof ApiException) {
            ApiException api = (ApiException) e;
            switch (api.getStatusCode()) {
                case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                    return "GEOFENCE_NOT_AVAILABLE (서비스 비활성)";
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                    return "GEOFENCE_TOO_MANY_GEOFENCES (최대 개수 초과)";
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                    return "GEOFENCE_TOO_MANY_PENDING_INTENTS (PendingIntent 너무 많음)";
                default:
                    return "알 수 없는 오류: " + api.getStatusCode();
            }
        }
        return "예외 발생: " + e.getMessage();
    }
}
