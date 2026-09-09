package com.urmitechnology.employeeapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.CookieManager;

import androidx.core.content.ContextCompat;

import java.util.Locale;

public class MainActivity extends Activity {

    private WebView webView;
    private TextToSpeech textToSpeech;
    private static final int LOCATION_REQUEST_CODE = 1001;

    private String pendingOrigin;
    private GeolocationPermissions.Callback pendingCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false); 
        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin,
                    GeolocationPermissions.Callback callback) {

                if (checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

                    pendingOrigin = origin;
                    pendingCallback = callback;

                    requestPermissions(
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            LOCATION_REQUEST_CODE
                    );

                } else {
                    callback.invoke(origin, true, false);
                }
            }
        });

        setContentView(webView);
        Intent serviceIntent =
        new Intent(this, LocationService.class);

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

    ContextCompat.startForegroundService(
            this,
            serviceIntent
    );

} else {

    startService(serviceIntent);

}

        textToSpeech = new TextToSpeech(
        this,
        status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        }
);
        webView.addJavascriptInterface(
        new AndroidTTS(),
        "AndroidTTS"
);

        webView.loadUrl(
                "https://urmitechnology.in/emp_movement/employee/login"
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_REQUEST_CODE) {

            boolean granted =
                    grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (pendingCallback != null) {
                pendingCallback.invoke(
                        pendingOrigin,
                        granted,
                        false
                );
            }

            pendingCallback = null;
            pendingOrigin = null;
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private class AndroidTTS {

    @JavascriptInterface
    public void speak(String text) {

        if (text == null || text.trim().isEmpty()) {
            return;
        }

        runOnUiThread(() -> {

            if (textToSpeech != null) {

                textToSpeech.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "navigation"
                );
            }
        });
    }

    @JavascriptInterface
    public void setJourney(
            long employeeId,
            long journeyLegId
    ) {

        getSharedPreferences(
                "employee_tracking",
                MODE_PRIVATE
        )
                .edit()
                .putLong("employee_id", employeeId)
                .putLong("journey_leg_id", journeyLegId)
                .apply();
    }

    @JavascriptInterface
    public void clearJourney() {

        getSharedPreferences(
                "employee_tracking",
                MODE_PRIVATE
        )
                .edit()
                .clear()
                .apply();
    }
}


}
