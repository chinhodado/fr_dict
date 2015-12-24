package chin.com.frdict;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MyDialog extends Activity {
    public static boolean active = false;
    public static MyDialog myDialog;
    public static WebView webView, webView2;
    public EditText edt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(Utility.LogTag, "MyDialog onCreate()");
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        myDialog = MyDialog.this;

        setContentView(R.layout.popup);

        // web views
        webView = (WebView) findViewById(R.id.webView1);
        webView2 = (WebView) findViewById(R.id.webView2);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(myDialog, description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadResource(WebView view, String url){
                Pattern pattern = Pattern.compile("http(s?)://en\\.(m\\.)?wiktionary\\.org/wiki/(.*)#French");
                Matcher matcher = pattern.matcher(url);
                if(matcher.find()){
                    String word = matcher.group(3);
                    try {
                        word = URLDecoder.decode(word, "UTF-8");
                        new SearchWordAsyncTask(MyDialog.this, MyDialog.webView2, ChatHeadService.oxfordHachetteDb, word, false).execute();
                        MyDialog.myDialog.edt.setText(word);
                    } catch (UnsupportedEncodingException e) {
                        Log.w("frdict", "Error decoding word in URL");
                    }
                }
            }
        });
        webView2.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(myDialog, description, Toast.LENGTH_SHORT).show();
            }
        });

        // edit text
        edt = (EditText) findViewById(R.id.dialog_edt);
        edt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String str = edt.getText().toString();
                    if (str.length() > 0) {
                        new SearchWordAsyncTask(MyDialog.this, webView, ChatHeadService.wiktionaryDb, str, true)
                                .execute();
                        new SearchWordAsyncTask(MyDialog.this, webView2, ChatHeadService.oxfordHachetteDb, str, false)
                                .execute();
                    }

                    // hide the keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(edt.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        // search image
        final ImageView searchImg = (ImageView) findViewById(R.id.imageView_search);
        searchImg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = edt.getText().toString();
                if (str.length() > 0) {
                    new SearchWordAsyncTask(MyDialog.this, webView, ChatHeadService.wiktionaryDb, str, true).execute();
                    new SearchWordAsyncTask(MyDialog.this, webView2, ChatHeadService.oxfordHachetteDb, str, false)
                            .execute();
                }

                // hide the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edt.getWindowToken(), 0);
            }
        });

        // invisible top section
        View top = (View) findViewById(R.id.dialog_top);
        top.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                moveTaskToBack(true);
            }
        });

        // dictionary tabs
        TextView wiktionaryTv = (TextView) findViewById(R.id.textViewLabelDict1);
        TextView oxfordTv = (TextView) findViewById(R.id.textViewLabelDict2);
        final View wiktionaryIndicator = findViewById(R.id.indicatorDict1);
        final View oxfordIndicator = findViewById(R.id.indicatorDict2);
        final ScrollView scrollView1 = (ScrollView) findViewById(R.id.scrollView1);
        final ScrollView scrollView2 = (ScrollView) findViewById(R.id.scrollView2);
        OnClickListener dictTabOnClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView) v;
                if (tv.getText().equals("Wiktionary")) {
                    wiktionaryIndicator.setVisibility(View.VISIBLE);
                    oxfordIndicator.setVisibility(View.INVISIBLE);
                    scrollView1.setVisibility(View.VISIBLE);
                    scrollView2.setVisibility(View.GONE);
                } else if (tv.getText().equals("Oxford Hachette")) {
                    wiktionaryIndicator.setVisibility(View.INVISIBLE);
                    oxfordIndicator.setVisibility(View.VISIBLE);
                    scrollView1.setVisibility(View.GONE);
                    scrollView2.setVisibility(View.VISIBLE);
                }
            }
        };
        wiktionaryTv.setOnClickListener(dictTabOnClickListener);
        oxfordTv.setOnClickListener(dictTabOnClickListener);
    }

    @Override
    protected void onResume() {
        Log.i(Utility.LogTag, "MyDialog onResume()");
        super.onResume();
        active = true;

        processIntent();
    }

    /**
     * Process the intent used to start up/bring up this activity
     */
    private void processIntent() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String str = bundle.getString("FromClipboard");
            if (str != null && !str.equals("")) {
                new SearchWordAsyncTask(MyDialog.this, webView, ChatHeadService.wiktionaryDb, str, true).execute();
                new SearchWordAsyncTask(MyDialog.this, webView2, ChatHeadService.oxfordHachetteDb, str, false)
                        .execute();
                edt.setText(str);
            }
        }
    }

    /**
     * Needed since this activity is a singleTask one
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processIntent();
    }

    @Override
    protected void onPause() {
        Log.i(Utility.LogTag, "MyDialog onPause()");
        super.onPause();
        active = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(Utility.LogTag, "MyDialog onDestroy()");
        active = false;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
