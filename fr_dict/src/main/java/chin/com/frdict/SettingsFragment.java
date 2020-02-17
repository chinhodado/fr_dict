package chin.com.frdict;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

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

        Preference chatheadSizePref = findPreference("pref_chatheadSize");
        chatheadSizePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                RelativeLayout chatheadView = ChatHeadService.INSTANCE.getChatheadView();

                if (chatheadView != null) {
                    ViewGroup.LayoutParams layoutParams = chatheadView.getLayoutParams();
                    layoutParams.width = (int) newValue;
                    layoutParams.height = (int) newValue;
                    WindowManager windowManager = ChatHeadService.INSTANCE.getWindowManager();
                    windowManager.removeView(chatheadView);
                    windowManager.addView(chatheadView, layoutParams);
                }

                return true;
            }
        });

        Preference versionPref = findPreference("pref_version");
        versionPref.setSummary(BuildConfig.VERSION_NAME);

        Preference gitPref = findPreference("pref_git");
        gitPref.setSummary(BuildConfig.GIT_HASH);

        Preference timeAdapterPref = findPreference("pref_createAdapterTime");
        timeAdapterPref.setSummary(ChatHeadService.INSTANCE.getCreateAdapterTime() + "ms");
    }
}
