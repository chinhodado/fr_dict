package chin.com.frdict.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import chin.com.frdict.R;
import chin.com.frdict.database.AppDatabase;
import chin.com.frdict.database.CountedSearchItem;

public class RecentSearchActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        new AsyncTask<Void, Void, List<String>>() {

            @Override
            protected List<String> doInBackground(Void... params) {
                // Create a list data which will be displayed in inner ListView.
                List<String> listData = new ArrayList<>();

                AppDatabase db = AppDatabase.getDatabase(DictionaryActivity.INSTANCE);
                List<CountedSearchItem> allSearches = db.recentSearchDao().getAllDistinct();

                for (CountedSearchItem item : allSearches) {
                    listData.add(item.getText() + " (" + item.getCount() + ")");
                }

                return listData;
            }

            @Override
            protected void onPostExecute(List<String> listData) {
                ArrayAdapter<String> listDataAdapter = new ArrayAdapter<>(RecentSearchActivity.this, android.R.layout.simple_list_item_1, listData);

                setListAdapter(listDataAdapter);
            }
        }.execute();

    }

    // When user click list item, this method will be invoked.
    @Override
    protected void onListItemClick(ListView listView, View v, int position, long id) {
        ListAdapter listAdapter = listView.getAdapter();
        Object selectItemObj = listAdapter.getItem(position);
        String itemText = (String)selectItemObj;

        // Create an AlertDialog to show.
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setMessage(itemText);
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_recent_search, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.menu_recent_search_close) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
