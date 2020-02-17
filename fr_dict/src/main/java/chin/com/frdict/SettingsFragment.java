package chin.com.frdict;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

/**
 * Settings Fragment
 * <p>
 * Created by Chin on 20-Nov-16.
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Preference versionPref = findPreference("pref_version");
        versionPref.setSummary(BuildConfig.VERSION_NAME);

        Preference gitPref = findPreference("pref_git");
        gitPref.setSummary(BuildConfig.GIT_HASH);

        Preference timeAdapterPref = findPreference("pref_createAdapterTime");
        timeAdapterPref.setSummary(ChatHeadService.INSTANCE.getCreateAdapterTime() + "ms");
    }
}
