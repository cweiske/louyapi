package de.cweiske.ouya.louyapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.Buffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;

public class MainActivity extends Activity {

    private static final String TAG = "louyapi-ui";

    protected Intent serviceIntent;

    protected String configFilePath;
    protected String configFileBackupPath;
    protected String gameDataVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent = new Intent(this, HttpService.class);
        startService(serviceIntent);

        configFilePath = Environment.getExternalStorageDirectory().getPath() + "/ouya_config.properties";
        configFileBackupPath = Environment.getExternalStorageDirectory().getPath() + "/ouya_config.properties.backup";

        loadGameDataVersion();
        loadStatus();
    }

    protected void loadGameDataVersion() {
        try {
            InputStream is = getAssets().open("stouyapi-www/game-data-version", AssetManager.ACCESS_BUFFER);
            gameDataVersion = new BufferedReader(new InputStreamReader(is)).readLine();
        } catch (IOException e) {
            gameDataVersion = "unknown";
        }
    }

    protected void loadStatus() {
        TextView statusGameDataVersion = (TextView) findViewById(R.id.statusGameDataVersion);
        statusGameDataVersion.setText(gameDataVersion);

        TextView statusPortNumber = (TextView) findViewById(R.id.statusPortNumber);
        statusPortNumber.setText("8080");

        TextView statusCurrentServer = (TextView) findViewById(R.id.statusCurrentServer);
        String currentServer = loadCurrentServer();
        if (currentServer == null) {
            statusCurrentServer.setText("https://devs.ouya.tv/ (default)");
        } else {
            statusCurrentServer.setText(currentServer);
        }

        TextView statusIpAddress = (TextView) findViewById(R.id.statusIpAddress);
        statusIpAddress.setText(getCurrentIpAddressStatus());

        Button useDefault = (Button) findViewById(R.id.buttonUseDefault);
        Button backupCreate = (Button) findViewById(R.id.buttonBackupCreate);
        File conf = new File(configFilePath);
        useDefault.setEnabled(conf.exists());
        backupCreate.setEnabled(conf.exists());

        Button backupRestore = (Button) findViewById(R.id.buttonBackupRestore);
        TextView backupTime = (TextView) findViewById(R.id.backupDate);
        File backup = new File(configFileBackupPath);
        backupRestore.setEnabled(backup.exists());
        if (backup.exists()) {
            Date modified = new Date(backup.lastModified());
            SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss z");
            backupTime.setText(sdf.format(modified));
        } else {
            backupTime.setText("");
        }
    }

    /**
     * This got much more complicated than I anticipated :(
     *
     * @return The current IPv4 address plus the connection type
     */
    private String getCurrentIpAddressStatus() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return "Unknown";
        }

        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) {
            return "No network connection";
        }
        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            int ip = wifi != null ? wifi.getConnectionInfo().getIpAddress() : 0;
            if (ip != 0) {
                String ipStr = formatIp(ip);
                return ipStr + " (WiFi)";
            }
            return "WiFi";

        } else if (info.getType() == ConnectivityManager.TYPE_ETHERNET) {
            try {
                NetworkInterface eth0 = null;
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (networkInterface.getName().equals("eth0")) {
                        eth0 = networkInterface;
                    }
                }
                if (eth0 == null) {
                    return "unknown (Ethernet)";
                }

                Enumeration<InetAddress> addresses = eth0.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    //I found that this gave me the correct IPv4 address
                    if (address.isSiteLocalAddress()) {
                        return address.getHostAddress() + " (Ethernet)";
                    }
                }

                return "unknown (Ethernet)";
            } catch (SocketException e) {
                return "unknown (Ethernet)";
            }

        } else {
            return "unknown (" + info.getTypeName() + ")";
        }
    }

    protected String loadCurrentServer()
    {
        Properties props = new Properties();
        try {
            FileInputStream in = new FileInputStream(configFilePath);
            props.load(in);
        } catch (Exception e) {
            return null;
        }

        return props.getProperty("OUYA_SERVER_URL", null);
    }

    public void onClickExit(View view) {
        this.finish();
    }

    public void onClickBackupCreate(View view) {
        if (copyFile(configFilePath, configFileBackupPath)) {
            showInfo("Backup created.");
        }
        loadStatus();
    }

    public void onClickBackupRestore(View view) {
        if (copyFile(configFileBackupPath, configFilePath)) {
            showInfo("Backup restored.");
        }
        loadStatus();
    }

    private boolean copyFile(String source, String target) {
        try {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(target);
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            showError(e.getMessage());
            return false;
        }
        return true;
    }

    public void onClickUseDefault(View view) {
        File f = new File(configFilePath);
        if (f.delete()) {
            showInfo("Configuration file deleted");
        } else {
            showError("Could not delete configuration file");
        }
        loadStatus();
    }

    public void onClickUseLouyapi(View view) {
        writeConfig("http://127.0.0.1:8080/", "http://127.0.0.1:8080/api/v1/status");
        loadStatus();
    }

    public void onClickUseCweiske(View view) {
        writeConfig("http://ouya.cweiske.de/", "http://ouya.cweiske.de/api/v1/status");
        loadStatus();
    }

    private void writeConfig(String serverUrl, String statusUrl) {
        String content = "OUYA_SERVER_URL=" + serverUrl + "\n"
            + "OUYA_STATUS_SERVER_URL=" + statusUrl + "\n";
        try {
            FileOutputStream fos = new FileOutputStream(configFilePath);
            fos.write(content.getBytes());
            showInfo("Configuration written");
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    private void showInfo(String message) {
        showError(message);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected String formatIp(int ip) {
        return String.format(
            Locale.ENGLISH,
            "%d.%d.%d.%d",
            (ip & 0xff),
            (ip >> 8 & 0xff),
            (ip >> 16 & 0xff),
            (ip >> 24 & 0xff));
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