package chin.com.frdict;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.chin.common.RegexFilterArrayAdapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Filter;

/**
 * A specialized adapter for accent-insensitive filtering
 */
@SuppressLint("DefaultLocale")
public class AccentInsensitiveFilterArrayAdapter extends RegexFilterArrayAdapter<String> {

    protected AccentInsensitiveArrayFilter mFilter;
    private ArrayList<String> accentRemovedList;

    private static final Pattern DIACRITICS_AND_FRIENDS
        = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when instantiating views.
     * @param objects The objects to represent in the ListView.
     */
    public AccentInsensitiveFilterArrayAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);

        long start = System.currentTimeMillis();
        accentRemovedList = new ArrayList<String>(objects.size());
        int size = objects.size();

        // partitioning
        int numThread = Runtime.getRuntime().availableProcessors();
        Log.i("frdict", "AccentInsensitiveFilterArrayAdapter - using " + numThread + " threads for creating accent-removed list");
        final int CHUNK_SIZE = size / numThread;
        final int LAST_CHUNK = size - (numThread - 1) * CHUNK_SIZE; // last chunk can be a bit bigger

        List<List<String>> parts = new ArrayList<List<String>>();
        for (int i = 0; i < size - LAST_CHUNK; i += CHUNK_SIZE) {
            parts.add(new ArrayList<String>(
                objects.subList(i, i + CHUNK_SIZE))
            );
        }

        parts.add(new ArrayList<String>(
            objects.subList(size - LAST_CHUNK, size))
        );

        // processed parts
        List<List<String>> processedParts = new ArrayList<List<String>>();
        for (int i = 0; i < parts.size(); i ++) {
            processedParts.add(new ArrayList<String>(CHUNK_SIZE));
        }

        List<Thread> threadList = new ArrayList<Thread>();
        for (int i = 0; i < numThread; i++) {
            final List<String> workList = parts.get(i);
            final List<String> processedList = processedParts.get(i);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    for (int n = 0; n < workList.size(); n++) {
                        String word = workList.get(n);
                        processedList.add(stripDiacritics(word.toLowerCase()));
                    }
                }
            };
            Thread thread = new Thread(r);
            thread.start();
            threadList.add(thread);
        }

        for(Thread t : threadList) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // join processed parts together
        for (int i = 0; i < processedParts.size(); i ++) {
            accentRemovedList.addAll(processedParts.get(i));
        }

        long end = System.currentTimeMillis();
        long duration = (end - start);
        Log.i("frdict", "AccentInsensitiveFilterArrayAdapter - creating accentRemovedList time: " + duration + "ms");
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