package chin.com.frdict;

import android.content.Context;
import android.os.AsyncTask;
import android.webkit.WebView;

import chin.com.frdict.database.BaseDictionarySqliteDatabase;

public class SearchWordAsyncTask extends AsyncTask<Void, Void, String> {
    private WebView webView;
    private String word;
    private Context context;
    private BaseDictionarySqliteDatabase db;

    /**
     * Constructor
     * @param context A context
     * @param webView The webview to display the word definition
     * @param db The database helper to work with an offline database
     * @param word The word to look up
     */
    public SearchWordAsyncTask(Context context, WebView webView, BaseDictionarySqliteDatabase db, String word) {
        this.context = context;
        this.webView = webView;
        this.word = word;
        this.db = db;
    }

    @Override
    protected String doInBackground(Void... params) {
        return getWordDefinitionOffline();
    }

    @Override
    protected void onPostExecute(String html) {
        webView.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");
    }

    private String getWordDefinitionOffline() {
        String definition = db.getWordDefinition(word);
        if (definition == null) {
            definition = "Word not found: " + word;
        }
        return definition;
    }
}
