package chin.com.frdict;

import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import chin.com.frdict.activity.DictionaryActivity;

/**
 * Custom WebViewClient for the app, most notably supports the frdict:// protocol.
 * <p>
 * Created by Chin on 05-Nov-16.
 */
public class FrDictWebViewClient extends WebViewClient {
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Toast.makeText(DictionaryActivity.INSTANCE, description, Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle our own protocol when clicking on such a link in the WebView
     * TODO: implement a system-wide intent filter for the custom protocol?
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url != null && url.startsWith("frdict:/")) {
            Uri parsed = Uri.parse(url);
            String word = parsed.getQueryParameter("word");

            String highlight = parsed.getQueryParameter("highlight");
            String deepSearch = parsed.getQueryParameter("deepSearch");
            SearchManager searchManager = ChatHeadService.INSTANCE.getSearchManager();
            if (deepSearch != null) {
                searchManager.deepSearch(word);
            }
            else if (highlight == null) {
                searchManager.searchWord(word);
            }
            else {
                searchManager.searchWordAndHighlight(word, highlight);
            }

            DictionaryActivity.INSTANCE.edt.setText(word);
            return true;
        }
        else {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        // note: when we search a word and it reaches here, the url is just "about:blank"
        // so don't do any url parsing here
        view.loadUrl("javascript:if (typeof scrollToHighlight === 'function') scrollToHighlight();");
    }
}
