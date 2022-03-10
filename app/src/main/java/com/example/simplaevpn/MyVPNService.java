package com.example.simplaevpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MyVPNService extends VpnService {

    public static final String EXTRA_COMMAND = "Command";
    private static final String EXTRA_REASON = "Reason";
    public static final String TAG = "VPN_SERVICE";
    private final AtomicReference<Connection> mConnection = new AtomicReference<>();
    private AtomicInteger mNextConnectionId = new AtomicInteger(1);

    private Handler mHandler;
    private static class Connection extends Pair<Thread, ParcelFileDescriptor> {
        public Connection(Thread thread, ParcelFileDescriptor pfd) {
            super(thread, pfd);
        }
    }


    private PendingIntent mConfigureIntent;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        if (mHandler == null) {
            mHandler = new Handler();
        }
        mConfigureIntent = PendingIntent.getActivity(this, 0, new Intent(this, MyVPNService.class)
        , PendingIntent.FLAG_UPDATE_CURRENT
        );
        Log.d(TAG, "on_create called ");
        super.onCreate();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getWaitingNotification() {
        final String NOTIFICATION_CHANNEL_ID = "ToyVpn";
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        mNotificationManager.createNotificationChannel(new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT));
        startForeground(1, new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentText("VPN STARTED")
                .setContentIntent(mConfigureIntent)
                .build());
    }
    public static final String ACTION_DISCONNECT = "com.example.android.toyvpn.STOP";


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent !=null && ACTION_DISCONNECT.equals(intent.getAction())) {
            disconnect();
            return START_NOT_STICKY;
        } else {
            connect();
            return START_STICKY;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void connect() {
        getWaitingNotification();
        mHandler.sendEmptyMessage(5);
        String server = "10.0.0.2";
        int port = 0;
        String proxyHost = "0.0.0.0";
        int proxyPort = 0;
        boolean allow = true;
        final byte[] secret = "".getBytes();
        String[] mpackage = {
                "com.android.chrome",
                "com.google.android.youtube",
                "com.example.a.missing.app"};
        Set<String> packages = new HashSet<String>();
        Collections.addAll(packages, mpackage);

        startConnection(new ToyVpnConnection(this,mNextConnectionId.getAndIncrement(), server, port, secret, proxyHost, proxyPort, allow, packages));
    }
    private final AtomicReference<Thread> mConnectingThread = new AtomicReference<>();

    private void setConnectingThread(final Thread thread) {
        final Thread oldThread = mConnectingThread.getAndSet(thread);
        if (oldThread != null) {
            oldThread.interrupt();
        }
    }

    private void disconnect() {
        mHandler.sendEmptyMessage(6);
        setConnectingThread(null);
        setConnection(null);
        stopForeground(true);
    }

    private void startConnection(final ToyVpnConnection connection) {
        final Thread thread = new Thread(connection, "ToyVpnThread");
        setConnectingThread(thread);
        // Handler to mark as connected once onEstablish is called.
        connection.setConfigureIntent(mConfigureIntent);
        connection.setOnEstablishListener(tunInterface -> {
            mHandler.sendEmptyMessage(5);
            mConnectingThread.compareAndSet(thread, null);
            setConnection(new Connection(thread, tunInterface));
        });
        thread.start();

    }

    private void setConnection(final Connection connection) {
        final Connection oldConnection = mConnection.getAndSet(connection);
        if (oldConnection != null) {
            try {
                oldConnection.first.interrupt();
                oldConnection.second.close();
            } catch (IOException e) {
                Log.e(TAG, "Closing VPN interface", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();

    }

    public static void start(String reason, Context context) {
        Intent intent = new Intent(context, MyVPNService.class);
       // intent.putExtra(EXTRA_COMMAND,Command.start);
        intent.putExtra(EXTRA_REASON, reason);
        ContextCompat.startForegroundService(context, intent);
    }



}
