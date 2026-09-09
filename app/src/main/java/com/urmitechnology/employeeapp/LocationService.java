package com.urmitechnology.employeeapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
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

public class LocationService extends Service {

    private static final String CHANNEL_ID =
            "employee_location_channel";

    private FusedLocationProviderClient fusedLocationClient;

    private final LocationCallback locationCallback =
            new LocationCallback() {

                @Override
                public void onLocationResult(
                        LocationResult result) {

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

                        // Abhi yahan sirf location mil rahi hai.
                        // Next step me tumhare existing
                        // backend/API ko ye location bhejenge.

                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle(
                                "Employee Location Active"
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

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Employee Location",
                            NotificationManager
                                    .IMPORTANCE_LOW
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
