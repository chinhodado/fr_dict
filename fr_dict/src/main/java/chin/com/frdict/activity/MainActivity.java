package chin.com.frdict.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import chin.com.frdict.ChatHeadService;

/**
 * Just an activity to start the service from the launcher, and do nothing more
 * @author Chin
 */
public class MainActivity extends Activity {
    static final int OVERLAY_PERMISSION_REQ_CODE = 128;
    static final int EXTERNAL_STORAGE_PERMISSION_REQ_CODE = 256;

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please enable \"Permit drawing over other apps\" then run this app again", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            }
            else {
                checkAndRequestPermission();
            }
        }
        else {
            // no need to worry about pesky permission stuffs
            startService();
        }

        // immediately closes itself
        finish();
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkAndRequestPermission() {
        int permissionExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionExternalStorage != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, EXTERNAL_STORAGE_PERMISSION_REQ_CODE);
        }
        else {
            startService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSION_REQ_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    startService();
                } else {
                    // permission denied, boo!
                    Toast.makeText(this, "No permission, no dictionary for you", Toast.LENGTH_SHORT).show();
                }
                finish();
                return;
            }
            default:
                break;
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        // Just to keep Android Studio happy
        assert manager != null;

        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startService() {
        if (!isMyServiceRunning(ChatHeadService.class)) {
            Log.i("frdict", "Service is not running, starting service");
            startService(new Intent(MainActivity.this, ChatHeadService.class));
        }
        else {
            Log.i("frdict", "Service is already running");

            // if the service is already running, we should show the dict activity
            if (!DictionaryActivity.active) {
                Intent it = new Intent(ChatHeadService.INSTANCE, DictionaryActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                ChatHeadService.INSTANCE.startActivity(it);
            }
        }
    }
}
