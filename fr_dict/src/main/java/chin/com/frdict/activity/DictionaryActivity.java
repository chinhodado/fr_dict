package chin.com.frdict.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import java.util.List;
import java.util.Locale;

import chin.com.frdict.AccentInsensitiveFilterArrayAdapter;
import chin.com.frdict.ChatHeadService;
import chin.com.frdict.R;
import chin.com.frdict.Utility;
import chin.com.frdict.database.AppDatabase;
import chin.com.frdict.database.SearchItem;
import chin.com.frdict.tab.DictionaryPagerAdapter;
import chin.com.frdict.tab.PagerSlidingTabStrip;

public class DictionaryActivity extends FragmentActivity {
    public static volatile boolean active = false;

    @SuppressLint("StaticFieldLeak")
    public static DictionaryActivity INSTANCE;

    @SuppressLint("StaticFieldLeak")
    public static WebView webViewWiktionary, webViewOxfordHachette;

    public AutoCompleteTextView edt;
    private TextToSpeech tts;
    private View top;

    public enum Dictionary {
        Wiktionary, OxfordHachette
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(Utility.LogTag, "DictionaryActivity onCreate()");
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        INSTANCE = DictionaryActivity.this;

        setContentView(R.layout.activity_dict);

        // edit text
        edt = findViewById(R.id.dialog_edt);
        AccentInsensitiveFilterArrayAdapter adapter = ChatHeadService.INSTANCE.getAdapter();
        if (adapter != null) {
            edt.setAdapter(adapter);
        }
        edt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String str = edt.getText().toString();
                if (!str.isEmpty()) {
                    ChatHeadService.INSTANCE.getSearchManager().searchWord(str);
                }

                // hide the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null; // Keep Android Studio happy
                imm.hideSoftInputFromWindow(edt.getWindowToken(), 0);
                return true;
            }
            return false;
        });
        edt.setOnItemClickListener((parent, view, position, id) -> {
            String name = (String)parent.getItemAtPosition(position);
            ChatHeadService.INSTANCE.getSearchManager().searchWord(name);

            // hide the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null; // Keep Android Studio happy
            imm.hideSoftInputFromWindow(edt.getWindowToken(), 0);
        });

        // search image
        final ImageView searchImg = findViewById(R.id.imageView_search);
        searchImg.setOnClickListener(v -> {
            String str = edt.getText().toString();
            if (!str.isEmpty()) {
                ChatHeadService.INSTANCE.getSearchManager().searchWord(str);
            }

            // hide the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null; // Keep Android Studio happy
            imm.hideSoftInputFromWindow(edt.getWindowToken(), 0);
        });

        // deep search image
        final ImageView deepSearchImg = findViewById(R.id.imageView_deepSearch);
        deepSearchImg.setOnClickListener(v -> {
            String str = edt.getText().toString();
            if (!str.isEmpty()) {
                ChatHeadService.INSTANCE.getSearchManager().deepSearch(str);
            }

            // hide the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null; // Keep Android Studio happy
            imm.hideSoftInputFromWindow(edt.getWindowToken(), 0);
        });

        // invisible top section
        top = findViewById(R.id.dialog_top);
        top.setOnClickListener(v -> moveTaskToBack(true));

        // fullscreen image
        final ImageView menuImg = findViewById(R.id.imageView_menu);
        menuImg.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(INSTANCE, v);
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menu_chathead_focus:
                        ChatHeadService.INSTANCE.toggleChatheadFocus();
                        return true;
                    case R.id.menu_searchhistory:
                        onSearchHistoryMenuItemSelected();
                        return true;
                    case R.id.menu_fullscreen:
                        toggleFullScreen();
                        return true;
                    case R.id.menu_setting:
                        ChatHeadService.INSTANCE.openSettingActivity();
                        return true;
                    case R.id.menu_exit:
                        ChatHeadService.INSTANCE.exit();
                        return true;
                    default:
                        return false;
                }
            });
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu, popup.getMenu());
            popup.show();
        });

        // speaker image
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                // Using FRENCH to just specify the language. If specifically want
                // the language as spoken in the country, use FRANCE.
                tts.setLanguage(Locale.FRENCH);
            }
        });
        ImageView speaker = findViewById(R.id.imageView_speaker);
        speaker.setOnClickListener(v -> {
            String toSpeak = edt.getText().toString();
            tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
        });

        // tabs
        PagerSlidingTabStrip tabs = findViewById(R.id.tabs);
        ViewPager pager = findViewById(R.id.pager);
        DictionaryPagerAdapter pagerAdapter = new DictionaryPagerAdapter(getSupportFragmentManager());

        pager.setAdapter(pagerAdapter);

        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setShouldExpand(true); // note: has to be before setViewPager()
        tabs.setViewPager(pager);
        tabs.setIndicatorColor(ContextCompat.getColor(this, R.color.red));
    }

    private void onSearchHistoryMenuItemSelected() {
        Intent it = new Intent(ChatHeadService.INSTANCE, RecentSearchActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ChatHeadService.INSTANCE.startActivity(it);
    }

    public void toggleFullScreen() {
        int visibility = top.getVisibility();
        if (visibility == View.GONE) {
            top.setVisibility(View.VISIBLE);
        }
        else if (visibility == View.VISIBLE) {
            top.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        Log.i(Utility.LogTag, "DictionaryActivity onResume()");
        super.onResume();
        active = true;

        AccentInsensitiveFilterArrayAdapter adapter = ChatHeadService.INSTANCE.getAdapter();
        if (adapter != null) {
            Adapter edtAdapter = edt.getAdapter();
            if (edtAdapter == null) {
                edt.setAdapter(adapter);
            }
        }

        // If the WebViews are null, the fragments are not created yet, so we left it to them
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
            if (str != null && !str.isEmpty()) {
                ChatHeadService.INSTANCE.getSearchManager().searchWord(str);
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

        // stop the text-to-speech
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
