package chin.com.frdict;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

/**
 * Just an activity to start the service from the launcher, and do nothing more
 * @author Chin
 */
public class MainActivity extends Activity {
    static boolean serviceRegistered = false;
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
            // start the chat head service if it's not started already
            if (!serviceRegistered) {
                startService(new Intent(MainActivity.this, ChatHeadService.class));
                serviceRegistered = true;
            }
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
            if (!serviceRegistered) {
                startService(new Intent(MainActivity.this, ChatHeadService.class));
                serviceRegistered = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSION_REQ_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    // start the chat head service if it's not started already
                    if (!serviceRegistered) {
                        startService(new Intent(MainActivity.this, ChatHeadService.class));
                        serviceRegistered = true;
                    }
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
}
