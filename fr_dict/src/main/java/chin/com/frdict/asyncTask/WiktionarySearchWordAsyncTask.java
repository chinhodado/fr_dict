package chin.com.frdict.asyncTask;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chin.com.frdict.database.BaseDictionarySqliteDatabase;

/**
 * Specialized task for searching wiktionary
 * <p>
 * Created by Chin on 30-Jan-17.
 */
public class WiktionarySearchWordAsyncTask extends SearchWordAsyncTask {
    public WiktionarySearchWordAsyncTask(Context context, WebView webView, BaseDictionarySqliteDatabase db, String word) {
        super(context, webView, db, word);
    }

    public WiktionarySearchWordAsyncTask(Context context, WebView webView, BaseDictionarySqliteDatabase db, String word, String highlight) {
        super(context, webView, db, word, highlight);
    }

    @Override
    protected String doInBackground(Void... params) {
        String definition = db.getWordDefinition(word);

        boolean searchSecondWord = false;
        String secondWord = null;
        if (definition != null) {
            // In the current word's definition, if all the links all point to the same word,
            // append that word's definition to the current definition. This is useful for word
            // forms (e.g. conjugated verbs, plural form of nouns, etc.)
            Pattern p = Pattern.compile("<i>(\\w+)</i>");
            Matcher m = p.matcher(definition);
            Set<String> secondWords = new HashSet<>();
            while (m.find()) {
                String tmpWord = m.group(1);
                if (Objects.equals(tmpWord, "plural") || Objects.equals(tmpWord, "feminine")) {
                    continue;
                }
                secondWords.add(tmpWord);
            }

            if (secondWords.size() == 1) {
                secondWord = (String)secondWords.toArray()[0];
                String secondDefinition = db.getWordDefinition(secondWord);
                definition += secondDefinition;
                searchSecondWord = true;
            }

            String css = "<style> a { color: rgba(54, 95, 145, 1); } </style>";
            // this is just a heuristic and won't be accurate in all cases
            definition = definition.replaceAll("<i>(\\w+)</i>", "<a href='frdict://search\\?word=$1'><i>$1</i></a>");
            definition = css + definition;
        }

        if (definition == null) {
            definition = "Word not found: " + word;
        }

        // use the queue to signal the other tasks
        try {
            if (searchSecondWord) {
                queue.put(secondWord);
            }
            else {
                queue.put("");
            }
        }
        catch (InterruptedException e) {
            Log.i("frdict", "WiktionarySearchWordAsyncTask was interrupted");
        }

        return definition;
    }
}
