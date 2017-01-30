package teefourteen.glideplayer.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewGroupCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import teefourteen.glideplayer.dialogs.NeedPermissionsDialog;
import teefourteen.glideplayer.R;
import teefourteen.glideplayer.services.LibraryService;

public class SplashActivity extends AppCompatActivity {
    public static final String LIBRARY_INIT_THREAD_NAME = "lib-init-thread";
    final static public int REQUEST_READ_EXTERNAL_STORAGE = 1;
    final public static String LIBRARY_INITIALIZED_ACTION = "library_initialized";
    private BroadcastReceiver libInitCompleteReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        libInitCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent mainActivityIntent = new Intent(context, MainActivity.class);
                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainActivityIntent);
                finish();
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(libInitCompleteReceiver,
                new IntentFilter("library_initialized"));

        setContentView(R.layout.activity_splash);

        findViewById(R.id.splashContainer).getWidth();

        findViewById(R.id.test_sender_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(new Intent(getApplicationContext(), LibraryService.class));
                intent.putExtra("TEST_Mode_Sender", "");
                ((ViewGroup) findViewById(R.id.test_layout_lib).getParent()).removeView(findViewById(R.id.test_layout_lib));
                startService(intent);
            }
        });

        findViewById(R.id.test_receiver_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ((EditText) findViewById(R.id.test_ip_address)).getText().toString();
                Intent intent = new Intent(new Intent(getApplicationContext(), LibraryService.class));
                intent.putExtra("TEST_Mode_Receiver", ip);
                ((ViewGroup) findViewById(R.id.test_layout_lib).getParent()).removeView(findViewById(R.id.test_layout_lib));
                startService(intent);
            }
        });

        findViewById(R.id.test_button_neither).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(new Intent(getApplicationContext(), LibraryService.class));
                ((ViewGroup) findViewById(R.id.test_layout_lib).getParent()).removeView(findViewById(R.id.test_layout_lib));
                startService(intent);
            }
        });
    }

    private boolean readPermissionGranted() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
            return false;
        }
        else
            return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if(requestCode==REQUEST_READ_EXTERNAL_STORAGE && grantResults.length>0
                    && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                startService(new Intent(this, LibraryService.class));
            else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.currentThread().sleep(2000);
                        } catch (InterruptedException e) {}
                        new NeedPermissionsDialog().show(getSupportFragmentManager(), "new_alert_dialog");
                    }
                }).start();
            }
        }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(libInitCompleteReceiver);
        super.onDestroy();
    }
}
