package chin.com.frdict.tab;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
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