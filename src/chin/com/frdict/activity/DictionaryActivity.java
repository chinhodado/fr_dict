package chin.com.frdict.activity;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import chin.com.frdict.ChatHeadService;
import chin.com.frdict.PagerSlidingTabStrip;
import chin.com.frdict.R;
import chin.com.frdict.SearchWordAsyncTask;
import chin.com.frdict.Utility;

public class DictionaryActivity extends FragmentActivity {
    public static boolean active = false;
    public static DictionaryActivity instance;
    public static WebView webViewWiktionary, webViewOxfordHachette;
    public AutoCompleteTextView edt;

    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private MyPagerAdapter adapter;

    public enum Dictionary{
        Wiktionary, OxfordHachette
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(Utility.LogTag, "MyDialog onCreate()");
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        instance = DictionaryActivity.this;

        setContentView(R.layout.activity_dict);

        // edit text
        edt = (AutoCompleteTextView) findViewById(R.id.dialog_edt);
        if (ChatHeadService.adapter != null) {
            edt.setAdapter(ChatHeadService.adapter);
        }
        edt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String str = edt.getText().toString();
                    if (str.length() > 0) {
                        new SearchWordAsyncTask(DictionaryActivity.this, webViewWiktionary, ChatHeadService.wiktionaryDb, str, true)
                                .execute();
                        new SearchWordAsyncTask(DictionaryActivity.this, webViewOxfordHachette, ChatHeadService.oxfordHachetteDb, str, false)
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
        edt.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = (String)parent.getItemAtPosition(position);
                new SearchWordAsyncTask(DictionaryActivity.this, DictionaryActivity.webViewWiktionary, ChatHeadService.wiktionaryDb, name, true).execute();
                new SearchWordAsyncTask(DictionaryActivity.this, DictionaryActivity.webViewOxfordHachette, ChatHeadService.oxfordHachetteDb, name, false).execute();
            }
        });

        // search image
        final ImageView searchImg = (ImageView) findViewById(R.id.imageView_search);
        searchImg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = edt.getText().toString();
                if (str.length() > 0) {
                    new SearchWordAsyncTask(DictionaryActivity.this, webViewWiktionary, ChatHeadService.wiktionaryDb, str, true).execute();
                    new SearchWordAsyncTask(DictionaryActivity.this, webViewOxfordHachette, ChatHeadService.oxfordHachetteDb, str, false)
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

        // tabs
        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new MyPagerAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);

        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setShouldExpand(true); // note: has to be before setViewPager()
        tabs.setViewPager(pager);
        tabs.setIndicatorColor(ContextCompat.getColor(this, R.color.red));
    }

    @Override
    protected void onResume() {
        Log.i(Utility.LogTag, "MyDialog onResume()");
        super.onResume();
        active = true;

        if (ChatHeadService.adapter == null) {
            Toast.makeText(this, "AutoCompleteTextView is not ready yet", Toast.LENGTH_SHORT).show();
        }
        else {
            Adapter adapter = edt.getAdapter();
            if (adapter == null) {
                edt.setAdapter(ChatHeadService.adapter);
            }
        }

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
                new SearchWordAsyncTask(DictionaryActivity.this, webViewWiktionary, ChatHeadService.wiktionaryDb, str, true).execute();
                new SearchWordAsyncTask(DictionaryActivity.this, webViewOxfordHachette, ChatHeadService.oxfordHachetteDb, str, false)
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

    public static class DictionaryTabFragment extends Fragment {
        private static final String TYPE = "TYPE";
        Dictionary type;

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
                webViewWiktionary = (WebView) view.findViewById(R.id.webView_dict);
                client = new WebViewClient() {
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        Toast.makeText(instance, description, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLoadResource(WebView view, String url){
                        Pattern pattern = Pattern.compile("http(s?)://en\\.(m\\.)?wiktionary\\.org/wiki/(.*)#French");
                        Matcher matcher = pattern.matcher(url);
                        if(matcher.find()){
                            String word = matcher.group(3);
                            try {
                                word = URLDecoder.decode(word, "UTF-8");
                                new SearchWordAsyncTask(DictionaryActivity.instance, DictionaryActivity.webViewOxfordHachette, ChatHeadService.oxfordHachetteDb, word, false).execute();
                                DictionaryActivity.instance.edt.setText(word);
                            } catch (UnsupportedEncodingException e) {
                                Log.w("frdict", "Error decoding word in URL");
                            }
                        }
                    }
                };
                webViewWiktionary.setWebViewClient(client);
            }
            else {
                webViewOxfordHachette = (WebView) view.findViewById(R.id.webView_dict);
                client = new WebViewClient() {
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        Toast.makeText(instance, description, Toast.LENGTH_SHORT).show();
                    }
                };
                webViewOxfordHachette.setWebViewClient(client);
            }

            return view;
        }
    }

    public class MyPagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = { "Wiktionary", "Oxford Hachette"};

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return DictionaryTabFragment.newInstance(Dictionary.Wiktionary);
            }
            else {
                return DictionaryTabFragment.newInstance(Dictionary.OxfordHachette);
            }
        }
    }
}
