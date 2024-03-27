package chin.com.frdict;

import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.fragment.app.FragmentActivity;
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

        Preference chatheadSizePref = findPreference(getString(R.string.pref_chatheadSize));
        assert chatheadSizePref != null;
        chatheadSizePref.setOnPreferenceChangeListener((preference, newValue) -> {
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
        });

        Preference versionPref = findPreference(getString(R.string.pref_version));
        assert versionPref != null;
        versionPref.setSummary(BuildConfig.VERSION_NAME);

        Preference gitPref = findPreference(getString(R.string.pref_git));
        assert gitPref != null;
        gitPref.setSummary(BuildConfig.GIT_HASH);

        Preference timeAdapterPref = findPreference(getString(R.string.pref_createAdapterTime));
        assert timeAdapterPref != null;
        timeAdapterPref.setSummary(ChatHeadService.INSTANCE.getCreateAdapterTime() + "ms");

        Preference closePref = findPreference(getString(R.string.pref_close));
        assert closePref != null;
        closePref.setOnPreferenceClickListener(preference -> {
            FragmentActivity activity = SettingsFragment.this.getActivity();
            if (activity != null) {
                activity.finish();
            }

            return false;
        });
    }
}
