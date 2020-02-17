package chin.com.frdict.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import chin.com.frdict.SettingsFragment;

/**
 * Settings Activity
 * <p>
 * Created by Chin on 20-Nov-16.
 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        SettingsFragment frag = new SettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, frag)
                .commit();
    }
}
