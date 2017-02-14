package chin.com.frdict;

import android.content.Context;
import android.os.AsyncTask;

import chin.com.frdict.activity.DictionaryActivity;
import chin.com.frdict.asyncTask.DeepSearchAsyncTask;
import chin.com.frdict.asyncTask.OxfordHachetteSearchWordAsyncTask;
import chin.com.frdict.asyncTask.SearchWordAsyncTask;
import chin.com.frdict.asyncTask.WiktionarySearchWordAsyncTask;
import chin.com.frdict.database.BaseDictionarySqliteDatabase;
import chin.com.frdict.database.OxfordHachetteSqliteDatabase;
import chin.com.frdict.database.WiktionarySqliteDatabase;

/**
 * Perform search by delegating work to asynctask
 *
 * Created by Chin on 01-Feb-17.
 */
public class SearchManager {
    private WiktionarySearchWordAsyncTask wiktionaryTask;
    private OxfordHachetteSearchWordAsyncTask oxfordTask;
    private BaseDictionarySqliteDatabase wiktionaryDb;
    private BaseDictionarySqliteDatabase oxfordDb;
    private Context context;

    public SearchManager(Context context, WiktionarySqliteDatabase wiktionaryDb, OxfordHachetteSqliteDatabase oxfordDb) {
        this.wiktionaryDb = wiktionaryDb;
        this.oxfordDb = oxfordDb;
        this.context = context;
    }

    public void searchWord(String word) {
        cancelTasks();
        wiktionaryTask = new WiktionarySearchWordAsyncTask(context, DictionaryActivity.webViewWiktionary, wiktionaryDb, word);
        wiktionaryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        oxfordTask = new OxfordHachetteSearchWordAsyncTask(context, DictionaryActivity.webViewOxfordHachette, oxfordDb, word);
        oxfordTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void searchWordAndHighlight(String word, String highlight) {
        cancelTasks();
        wiktionaryTask = new WiktionarySearchWordAsyncTask(context, DictionaryActivity.webViewWiktionary, wiktionaryDb, word, highlight);
        wiktionaryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        oxfordTask = new OxfordHachetteSearchWordAsyncTask(context, DictionaryActivity.webViewOxfordHachette, oxfordDb, word, highlight);
        oxfordTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void deepSearch(String toSearch) {
        cancelTasks();
        new DeepSearchAsyncTask(context, DictionaryActivity.webViewWiktionary, wiktionaryDb, toSearch)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new DeepSearchAsyncTask(context, DictionaryActivity.webViewOxfordHachette, oxfordDb, toSearch)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Cancel all running async tasks
     */
    public void cancelTasks() {
        if (wiktionaryTask != null) {
            wiktionaryTask.cancel(true);
        }

        if (oxfordTask != null) {
            oxfordTask.cancel(true);
        }

        SearchWordAsyncTask.getSignalQueue().clear();
    }
}
