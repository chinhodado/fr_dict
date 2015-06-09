package chin.com.frdict;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Just an activity to start the service from the launcher, and do nothing more
 * @author Chin
 */
public class MainActivity extends Activity {

    static boolean serviceRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // start the chat head service if it's not started already
        if (!serviceRegistered) {
            startService(new Intent(MainActivity.this, ChatHeadService.class));
            serviceRegistered = true;
        }

        // immediately closes itself
        finish();
    }
}
