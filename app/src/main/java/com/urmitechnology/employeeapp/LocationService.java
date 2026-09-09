package com.urmitechnology.employeeapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LocationService extends Service {

    private static final String CHANNEL_ID =
            "employee_location_channel";

    private static final String API_URL =
            "https://urmitechnology.in/emp_movement/api/tracking/sync-gps";

    private FusedLocationProviderClient fusedLocationClient;

    private SharedPreferences prefs;

    private final LocationCallback locationCallback =
            new LocationCallback() {

                @Override
                public void onLocationResult(LocationResult result) {

                    if (result == null) {
                        return;
                    }

                    android.location.Location location =
                            result.getLastLocation();

                    if (location != null) {

                        double latitude =
                                location.getLatitude();

                        double longitude =
                                location.getLongitude();

                        float accuracy =
                                location.getAccuracy();

                        sendLocationToServer(
                                latitude,
                                longitude,
                                accuracy
                        );
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences(
                "employee_tracking",
                MODE_PRIVATE
        );

        createNotificationChannel();

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle(
                                "Employee Tracking Active"
                        )
                        .setContentText(
                                "Location tracking is running"
                        )
                        .setSmallIcon(
                                android.R.drawable.ic_menu_mylocation
                        )
                        .setOngoing(true)
                        .build();

        startForeground(
                1001,
                notification
        );

        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        startLocationUpdates();
    }

    private void startLocationUpdates() {

        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf();
            return;
        }

        LocationRequest request =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        10000
                )
                        .setMinUpdateIntervalMillis(5000)
                        .build();

        fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                getMainLooper()
        );
    }

    private void sendLocationToServer(
            double latitude,
            double longitude,
            float accuracy
    ) {

        long employeeId =
                prefs.getLong("employee_id", 0);

        long journeyLegId =
                prefs.getLong("journey_leg_id", 0);

        if (employeeId == 0 || journeyLegId == 0) {
            return;
        }

        new Thread(() -> {

            try {

                URL url =
                        new URL(API_URL);

                HttpURLConnection connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("POST");

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setDoOutput(true);

                String timestamp =
                        new java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                java.util.Locale.getDefault()
                        ).format(
                                new java.util.Date()
                        );

                String json =
                        "{"
                        + "\"employee_id\":" + employeeId + ","
                        + "\"journey_leg_id\":" + journeyLegId + ","
                        + "\"points\":[{"
                        + "\"lat\":" + latitude + ","
                        + "\"lng\":" + longitude + ","
                        + "\"accuracy\":" + accuracy + ","
                        + "\"timestamp\":\"" + timestamp + "\""
                        + "}]"
                        + "}";

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        json.getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                );

                output.flush();
                output.close();

                connection.getResponseCode();

                connection.disconnect();

            } catch (Exception e) {

                e.printStackTrace();

            }

        }).start();
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Employee Location",
                            NotificationManager.IMPORTANCE_LOW
                    );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {
                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        if (fusedLocationClient != null) {

            fusedLocationClient
                    .removeLocationUpdates(
                            locationCallback
                    );
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
