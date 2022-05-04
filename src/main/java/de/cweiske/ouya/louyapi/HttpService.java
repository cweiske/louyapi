package de.cweiske.ouya.louyapi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class HttpService extends Service {
    NanoHTTPD server;

    static String TAG = "HttpService";

    @Override
    public void onCreate() {
        Log.i("service", "start service");
        super.onCreate();

        server = new HttpServer(8080, getAssets());
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException ioe) {
            //FIXME
            Log.e(TAG, "Couldn't start server:\n" + ioe);
            System.exit(-1);
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "stop service");
        super.onDestroy();
        server.stop();

        //restart the service
        sendBroadcast(new Intent(this, Autostart.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //android shall start the service again if killed
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "Task removed");
        super.onTaskRemoved(rootIntent);
    }
}