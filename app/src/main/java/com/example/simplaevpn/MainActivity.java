package com.example.simplaevpn;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.text.method.CharacterPickerDialog;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    public static final String EXTRA_SIZE = "Size";
    public static final String ACTION_QUEUE_CHANGED = "eu.faircode.netguard.ACTION_QUEUE_CHANGED";
    public static final String TAG = "Main";
    public static final int REQUEST_VPN = 1;
    private boolean isRunning = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isRunning = true;
        findViewById(R.id.start_vpn).setOnClickListener(view -> {
            startVpn();

        });
    }

    public static final int VPN_REQUEST = 125;

    void startVpn() {
     final Intent prepare = VpnService.prepare(this);
     if (prepare == null) {
         Log.i(TAG, "prepare done");
         onActivityResult(REQUEST_VPN, RESULT_OK, null);
     } else {
         if (isRunning) {
             Log.i(TAG, "Start intent = "+ prepare);
             try {
                 startActivityForResult(prepare, REQUEST_VPN);

             }catch (Throwable ex) {
                 Log.e(TAG, ex.toString()+ " "+ Log.getStackTraceString(ex));
                 onActivityResult(REQUEST_VPN, RESULT_CANCELED, null);

             }
         }

     }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
       if (requestCode == REQUEST_VPN) {
           if (resultCode == RESULT_OK) {
               Log.i(TAG, "result okay ");
               LocalVPNService.start("prepared", this);
           } else if (resultCode == RESULT_CANCELED) {
               Log.i(TAG, "VPN canceled");
           }
       }
    }
}
