package chin.com.frdict;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Settings Fragment
 *
 * Created by Chin on 20-Nov-16.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        String versionName = BuildConfig.VERSION_NAME;
        Preference versionPref = findPreference("pref_version");
        versionPref.setSummary(versionName);

        Preference timeAdapterPref = findPreference("pref_createAdapterTime");
        timeAdapterPref.setSummary(ChatHeadService.INSTANCE.getCreateAdapterTime() + "ms");
    }
}
