package chin.com.frdict.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import chin.com.frdict.ChatHeadService;
import chin.com.frdict.R;
import chin.com.frdict.Utility;
import chin.com.frdict.tab.DictionaryPagerAdapter;
import chin.com.frdict.tab.PagerSlidingTabStrip;

public class DictionaryActivity extends FragmentActivity {
    public static boolean active = false;
    public static DictionaryActivity instance;
    public static WebView webViewWiktionary, webViewOxfordHachette;
    public AutoCompleteTextView edt;

    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private DictionaryPagerAdapter adapter;

    public enum Dictionary{
        Wiktionary, OxfordHachette
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(Utility.LogTag, "DictionaryActivity onCreate()");
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
                        ChatHeadService.searchWord(str);
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
                ChatHeadService.searchWord(name);
            }
        });

        // search image
        final ImageView searchImg = (ImageView) findViewById(R.id.imageView_search);
        searchImg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = edt.getText().toString();
                if (str.length() > 0) {
                    ChatHeadService.searchWord(str);
                }

                // hide the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edt.getWindowToken(), 0);
            }
        });

        // invisible top section
        final View top = (View) findViewById(R.id.dialog_top);
        top.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                moveTaskToBack(true);
            }
        });

        // fullscreen image
        final ImageView fullscreenImg = (ImageView) findViewById(R.id.imageView_fullscreen);
        fullscreenImg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int visibility = top.getVisibility();
                if (visibility == View.GONE) {
                    top.setVisibility(View.VISIBLE);
                }
                else if (visibility == View.VISIBLE) {
                    top.setVisibility(View.GONE);
                }
            }
        });

        // tabs
        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new DictionaryPagerAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);

        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setShouldExpand(true); // note: has to be before setViewPager()
        tabs.setViewPager(pager);
        tabs.setIndicatorColor(ContextCompat.getColor(this, R.color.red));
    }

    @Override
    protected void onResume() {
        Log.i(Utility.LogTag, "DictionaryActivity onResume()");
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

        // if the webviews are null, the fragments are not created yet, so we left it to them
        // to handle the intent
        if (webViewOxfordHachette != null && webViewWiktionary != null) {
            processIntent();
        }
    }

    /**
     * Process the intent used to start up/bring up this activity
     */
    private void processIntent() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String str = bundle.getString(ChatHeadService.INTENT_FROM_CLIPBOARD);
            if (str != null && !str.equals("")) {
                ChatHeadService.searchWord(str);
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
    }

    @Override
    protected void onPause() {
        Log.i(Utility.LogTag, "DictionaryActivity onPause()");
        super.onPause();
        active = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(Utility.LogTag, "DictionaryActivity onDestroy()");
        active = false;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
