package chin.com.frdict;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.webkit.WebView;
import android.widget.ScrollView;
import chin.com.frdict.database.BaseDictionarySqliteDatabase;

public class SearchWordAsyncTask extends AsyncTask<Void, Void, String> {
    WebView webView;
    String word;
    Context context;
    BaseDictionarySqliteDatabase db;
    boolean exceptionOccurred = false;

    /**
     * Constructor
     * @param context A context
     * @param webView The webview to display the word definition
     * @param db The database helper to work with an offline database
     * @param word The word to look up
     * @param canOnline If this is false, word will always be looked up offline, no matter the network state
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
        displayWordOffline(html);

        // invalidate the parent ScrollView so that its height is reset and scroll it to the top
        ScrollView scrollView = (ScrollView)(webView.getParent());
        scrollView.invalidate();
        scrollView.fullScroll(View.FOCUS_UP);
    }

    private String getWordDefinitionOffline() {
        String definition = db.getWordDefinition(word);
        if (definition == null) {
            definition = "Word not found: " + word;
        }
        return definition;
    }

    private void displayWordOffline(String html) {
        webView.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");
    }
}
