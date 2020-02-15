package chin.com.frdict.asyncTask;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import chin.com.frdict.database.BaseDictionarySqliteDatabase;

/**
 * Specialized task for Oxford Hachette
 * <p>
 * Created by Chin on 01-Feb-17.
 */
public class OxfordHachetteSearchWordAsyncTask extends SearchWordAsyncTask {
    public OxfordHachetteSearchWordAsyncTask(Context context, WebView webView, BaseDictionarySqliteDatabase db, String word) {
        super(context, webView, db, word);
    }

    public OxfordHachetteSearchWordAsyncTask(Context context, WebView webView, BaseDictionarySqliteDatabase db, String word, String highlight) {
        super(context, webView, db, word, highlight);
    }

    @Override
    protected String doInBackground(Void... params) {
        String definition = db.getWordDefinition(word);

        try {
            String secondWord = queue.take();

            if (definition == null) {
                if (!secondWord.equals("")) {
                    definition = db.getWordDefinition(secondWord);

                    if (definition == null) {
                        definition = "Word not found: " + word;
                    }
                }
                else {
                    definition = "Word not found: " + word;
                }
            }

        } catch (InterruptedException e) {
            Log.i("frdict", "OxfordHachetteSearchWordAsyncTask was interrupted");
        }

        return definition;
    }
}
