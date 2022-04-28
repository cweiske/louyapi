package de.cweiske.ouya.louyapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

    protected Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent = new Intent(this, HttpService.class);
        startService(serviceIntent);
    }

    /**
     * Why do we want to stop exactly the service that we want to keep alive?
     * Because if we do not stop it, the service will die with our app.
     * Instead, by stopping the service, we will force the service to call its
     * own onDestroy which will force it to recreate itself after the app is dead.
     */
    protected void onDestroy() {
        stopService(serviceIntent);
        super.onDestroy();
    }
}