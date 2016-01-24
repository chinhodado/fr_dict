package chin.com.frdict.tab;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.widget.Toast;
import chin.com.frdict.ChatHeadService;
import chin.com.frdict.R;
import chin.com.frdict.SearchWordAsyncTask;
import chin.com.frdict.Utility;
import chin.com.frdict.activity.DictionaryActivity;
import chin.com.frdict.activity.DictionaryActivity.Dictionary;
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
        WebViewClient client = null;
        if (type == Dictionary.Wiktionary) {
            DictionaryActivity.webViewWiktionary = (WebView) view.findViewById(R.id.webView_dict);
            webview = DictionaryActivity.webViewWiktionary;
            dict = ChatHeadService.wiktionaryDb;
            client = new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Toast.makeText(DictionaryActivity.instance, description, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onLoadResource(WebView view, String url){
                    Pattern pattern = Pattern.compile("http(s?)://en\\.(m\\.)?wiktionary\\.org/wiki/(.*)#French");
                    Matcher matcher = pattern.matcher(url);
                    if(matcher.find()){
                        String word = matcher.group(3);
                        try {
                            word = URLDecoder.decode(word, "UTF-8");
                            new SearchWordAsyncTask(DictionaryActivity.instance, DictionaryActivity.webViewOxfordHachette, ChatHeadService.oxfordHachetteDb, word).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            DictionaryActivity.instance.edt.setText(word);
                        } catch (UnsupportedEncodingException e) {
                            Log.w("frdict", "Error decoding word in URL");
                        }
                    }
                }

                /**
                 * Handle our own protocol when clicking on such a link in the WebView
                 * TODO: implement a system-wide intent filter for the custom protocol?
                 */
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.startsWith("frdict://")) {
                        Pattern pattern = Pattern.compile("frdict://search\\?word=(.*)");
                        Matcher matcher = pattern.matcher(url);
                        if(matcher.find()){
                            try {
                                String word = matcher.group(1);
                                word = URLDecoder.decode(word, "UTF-8");
                                ChatHeadService.searchWord(word);
                                DictionaryActivity.instance.edt.setText(word);
                            }
                            catch (UnsupportedEncodingException e) {
                                Log.w("frdict", "Error decoding word in URL");
                            }
                        }
                        return true;
                    }
                    else {
                        return super.shouldOverrideUrlLoading(view, url);
                    }
                }
            };
            DictionaryActivity.webViewWiktionary.setWebViewClient(client);
        }
        else {
            DictionaryActivity.webViewOxfordHachette = (WebView) view.findViewById(R.id.webView_dict);
            webview = DictionaryActivity.webViewOxfordHachette;
            dict = ChatHeadService.oxfordHachetteDb;
            client = new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Toast.makeText(DictionaryActivity.instance, description, Toast.LENGTH_SHORT).show();
                }
            };
            DictionaryActivity.webViewOxfordHachette.setWebViewClient(client);
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(Utility.LogTag, "DictionaryTabFragment onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        String word = (String) activity.getIntent().getExtras().getString("FromClipboard");
        if (word != null) {
            new SearchWordAsyncTask(ChatHeadService.instance, webview, dict, word).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            DictionaryActivity.instance.edt.setText(word);
        }
    }
}