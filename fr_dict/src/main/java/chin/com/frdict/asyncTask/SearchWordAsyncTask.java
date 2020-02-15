package chin.com.frdict.asyncTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.webkit.WebView;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import chin.com.frdict.database.BaseDictionarySqliteDatabase;

/**
 * Base AsyncTask for searching a word, then display the definition in a WebView
 */
public class SearchWordAsyncTask extends AsyncTask<Void, Void, String> {
    @SuppressLint("StaticFieldLeak")
    protected final WebView webView;

    @SuppressLint("StaticFieldLeak")
    protected final Context context;

    protected final String word;
    protected final String highlight;
    protected final BaseDictionarySqliteDatabase db;
    protected static final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(1);

    /**
     * Constructor
     * @param context A context
     * @param webView The WebView to display the word definition
     * @param db The database helper to work with an offline database
     * @param word The word to look up
     */
    public SearchWordAsyncTask(Context context, WebView webView, BaseDictionarySqliteDatabase db, String word) {
        this(context, webView, db, word, null);
    }

    /**
     * Constructor
     * @param context A context
     * @param webView The WebView to display the word definition
     * @param db The database helper to work with an offline database
     * @param word The word to look up
     * @param highlight The string to highlight and scroll to in the word's definition
     */
    public SearchWordAsyncTask(Context context, WebView webView, BaseDictionarySqliteDatabase db, String word, String highlight) {
        this.context = context;
        this.webView = webView;
        this.word = word;
        this.highlight = highlight;
        this.db = db;
    }

    @Override
    protected String doInBackground(Void... params) {
        String definition = db.getWordDefinition(word);
        if (definition == null) {
            definition = "Word not found: " + word;
        }
        return definition;
    }

    @Override
    protected void onPostExecute(String html) {
        if (highlight != null) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("frdict")
                    .appendPath("search")
                    .appendQueryParameter("word", highlight)
                    .appendQueryParameter("deepSearch", "true");
            String myUrl = builder.build().toString();

            html = html.replaceAll("(?i)" + Pattern.quote(highlight),
                    "<span id='highlight' style='background-color: yellow'>" +
                            "<a style='color: inherit; text-decoration: inherit;' href='" + myUrl + "'>$0</a></span>");
            html += "<script> function scrollToHighlight() { window.location.hash = '#highlight';}</script>";
        }

        webView.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");

        // request focus for the WebView to avoid the autocomplete TextView popping up its list
        // (which is rather annoying when searching word copied from clipboard)
        webView.requestFocus();
    }

    public static BlockingQueue<String> getSignalQueue() {
        return queue;
    }
}
