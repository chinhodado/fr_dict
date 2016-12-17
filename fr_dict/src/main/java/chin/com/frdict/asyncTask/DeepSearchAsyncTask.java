package chin.com.frdict.asyncTask;

import android.content.Context;
import android.webkit.WebView;

import chin.com.frdict.database.BaseDictionarySqliteDatabase;

/**
 * Async task for deep search
 *
 * Created by Chin on 03-Nov-16.
 */
public class DeepSearchAsyncTask extends SearchWordAsyncTask {
    public DeepSearchAsyncTask(Context context, WebView webView, BaseDictionarySqliteDatabase db, String word) {
        super(context, webView, db, word);
    }

    @Override
    protected String getWordDefinitionOffline() {
        String definition = db.getDeepSearchResultsHtml(word, 100);
        if (definition == null) {
            definition = "Deep search not found: " + word;
        }
        return definition;
    }
}
