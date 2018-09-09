package chin.com.frdict.tab;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import chin.com.frdict.ChatHeadService;
import chin.com.frdict.FrdictWebViewClient;
import chin.com.frdict.R;
import chin.com.frdict.Utility;
import chin.com.frdict.activity.DictionaryActivity;
import chin.com.frdict.activity.DictionaryActivity.Dictionary;
import chin.com.frdict.asyncTask.OxfordHachetteSearchWordAsyncTask;
import chin.com.frdict.asyncTask.WiktionarySearchWordAsyncTask;
import chin.com.frdict.database.BaseDictionarySqliteDatabase;

public class DictionaryTabFragment extends Fragment {
    private static final String TYPE = "TYPE";
    Dictionary type;
    private WebView webview;
    private BaseDictionarySqliteDatabase dict;

    public static DictionaryTabFragment newInstance(Dictionary type) {
        DictionaryTabFragment f = new DictionaryTabFragment();
        Bundle b = new Bundle();
        b.putSerializable(TYPE, type);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = (Dictionary) getArguments().getSerializable(TYPE);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dict_tab, container, false);

        // web views
        if (type == Dictionary.Wiktionary) {
            DictionaryActivity.webViewWiktionary = view.findViewById(R.id.webView_dict);
            webview = DictionaryActivity.webViewWiktionary;
            dict = ChatHeadService.INSTANCE.getWiktionaryDb();
        }
        else {
            DictionaryActivity.webViewOxfordHachette = view.findViewById(R.id.webView_dict);
            webview = DictionaryActivity.webViewOxfordHachette;
            dict = ChatHeadService.INSTANCE.getOxfordHachetteDb();
        }

        webview.getSettings().setJavaScriptEnabled(true);
        WebViewClient client = new FrdictWebViewClient();
        webview.setWebViewClient(client);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(Utility.LogTag, "DictionaryTabFragment onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        String word = null;
        try {
            word = activity.getIntent().getExtras().getString(ChatHeadService.INTENT_FROM_CLIPBOARD);
        }
        catch (Exception e) {
            // activity can be null, or intent can be null, or extra can be null...
        }

        if (word != null) {
            // TODO: this is very bad! we're not managing the tasks through search manager
            // FIX THIS!
            if (type == Dictionary.Wiktionary) {
                new WiktionarySearchWordAsyncTask(ChatHeadService.INSTANCE, webview, dict, word)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else if (type == Dictionary.OxfordHachette) {
                new OxfordHachetteSearchWordAsyncTask(ChatHeadService.INSTANCE, webview, dict, word)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            DictionaryActivity.INSTANCE.edt.setText(word);
        }
    }
}