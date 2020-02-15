package chin.com.frdict.tab;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import chin.com.frdict.activity.DictionaryActivity.Dictionary;

public class DictionaryPagerAdapter extends FragmentPagerAdapter {

    private final String[] TITLES = { "Wiktionary", "Oxford Hachette"};

    public DictionaryPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return TITLES[position];
    }

    @Override
    public int getCount() {
        return TITLES.length;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return DictionaryTabFragment.newInstance(Dictionary.Wiktionary);
        }
        else {
            return DictionaryTabFragment.newInstance(Dictionary.OxfordHachette);
        }
    }
}