package chin.com.frdict;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.chin.common.RegexFilterArrayAdapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Filter;

/**
 * A specialized adapter for accent-insensitive filtering
 */
@SuppressLint("DefaultLocale")
public class AccentInsensitiveFilterArrayAdapter extends RegexFilterArrayAdapter<String> {

    protected AccentInsensitiveArrayFilter mFilter;
    private List<String> accentRemovedList;

    private static final Pattern DIACRITICS_AND_FRIENDS
        = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when instantiating views.
     * @param objects The objects to represent in the ListView.
     */
    public AccentInsensitiveFilterArrayAdapter(Context context, int resource, List<String> objects, List<String> accentRemovedList) {
        super(context, resource, objects);
        this.accentRemovedList = accentRemovedList;
    }

    private static String stripDiacritics(String str) {
        String tmp = Normalizer.normalize(str, Normalizer.Form.NFD);
        tmp = DIACRITICS_AND_FRIENDS.matcher(tmp).replaceAll("");
        return tmp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new AccentInsensitiveArrayFilter();
        }
        return mFilter;
    }

    /**
     * <p>An array filter constrains the content of the array adapter with
     * a search string. Each item that does not contain the search string
     * is removed from the list.</p>
     */
    private class AccentInsensitiveArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (mOriginalValues == null) {
                synchronized (mLock) {
                    mOriginalValues = new ArrayList<String>(mObjects);
                }
            }

            if (prefix == null || prefix.length() == 0) {
                ArrayList<String> list;
                synchronized (mLock) {
                    list = new ArrayList<String>(mOriginalValues);
                }
                results.values = list;
                results.count = list.size();
            }
            else {
                String filterString = stripDiacritics(prefix.toString().toLowerCase());

                ArrayList<String> values;
                synchronized (mLock) {
                    values = new ArrayList<String>(accentRemovedList);
                }

                final int count = values.size();
                final ArrayList<String> newValues = new ArrayList<String>();

                for (int i = 0; i < count; i++) {
                    final String value = values.get(i);

                    if (value.startsWith(filterString)) {
                        newValues.add(mOriginalValues.get(i));
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mObjects = (List<String>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}