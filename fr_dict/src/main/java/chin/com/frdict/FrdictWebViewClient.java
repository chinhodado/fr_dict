package chin.com.frdict;

import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chin.com.frdict.activity.DictionaryActivity;

/**
 * Custom WebViewClient for the app, most notably supports the frdict:// protocol.
 *
 * Created by Chin on 05-Nov-16.
 */
public class FrdictWebViewClient extends WebViewClient {
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Toast.makeText(DictionaryActivity.instance, description, Toast.LENGTH_SHORT).show();
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
}
