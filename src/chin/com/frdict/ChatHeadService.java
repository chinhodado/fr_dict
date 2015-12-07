package chin.com.frdict;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import chin.com.frdict.database.BaseDictionarySqliteDatabase;
import chin.com.frdict.database.OxfordHachetteSqliteDatabase;
import chin.com.frdict.database.WiktionarySqliteDatabase;

public class ChatHeadService extends Service {
    public static WindowManager windowManager;
    public RelativeLayout chatheadView, removeView;
    public static LinearLayout mainView;
    public ImageView removeImg;
    public Point szWindow = new Point();
    public static ChatHeadService instance;
    ClipboardManager clipMan;
    static boolean hasClipChangedListener = false;
    public static boolean mainViewVisible = false;
    public WebView webView, webView2;
    public EditText edt;

    // dictionaries
    BaseDictionarySqliteDatabase wiktionaryDb;
    BaseDictionarySqliteDatabase oxfordHachetteDb;

    /**
     * Event handler for looking up the word that was just copied into the clipboard
     */
    ClipboardManager.OnPrimaryClipChangedListener primaryClipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
        @Override
        public void onPrimaryClipChanged() {
            String str = (String) clipMan.getText();
            if (str != null && str.trim().length() > 0) {
                str = str.trim();
                if (str.contains("s'") || str.contains("s’")) {
                	str = str.replace("s'", "").replace("s’", "");
                }
                if (!mainViewVisible) {
                    mainView.setVisibility(View.VISIBLE);
                    mainViewVisible = true;
                }
                new SearchWordAsyncTask(ChatHeadService.this, webView, wiktionaryDb, str, true).execute();
                new SearchWordAsyncTask(ChatHeadService.this, webView2, oxfordHachetteDb, str, false).execute();
                edt.setText(str);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Utility.LogTag, "ChatHeadService.onCreate()");
        instance = this;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        wiktionaryDb = WiktionarySqliteDatabase.getInstance(this);
        oxfordHachetteDb = OxfordHachetteSqliteDatabase.getInstance(this);

        // the remove view
        removeView = (RelativeLayout) inflater.inflate(R.layout.remove, null);
        WindowManager.LayoutParams paramRemove = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        paramRemove.gravity = Gravity.TOP | Gravity.START;
        removeView.setVisibility(View.GONE);
        removeImg = (ImageView) removeView.findViewById(R.id.remove_img);
        windowManager.addView(removeView, paramRemove);

        // main view
        mainView = (LinearLayout) inflater.inflate(R.layout.popup, null);
        windowManager.getDefaultDisplay().getSize(szWindow);
        WindowManager.LayoutParams mainViewParams = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        windowManager.addView(mainView, mainViewParams);
        mainViewVisible = true;

        // main view - web views
        webView = (WebView) mainView.findViewById(R.id.webView1);
        webView2 = (WebView) mainView.findViewById(R.id.webView2);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(ChatHeadService.this, description, Toast.LENGTH_SHORT).show();
            }
        });
        webView2.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(ChatHeadService.this, description, Toast.LENGTH_SHORT).show();
            }
        });

        // main view - edit text
        edt = (EditText) mainView.findViewById(R.id.dialog_edt);
        edt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String str = edt.getText().toString();
                    if (str.length() > 0) {
                        new SearchWordAsyncTask(ChatHeadService.this, webView, wiktionaryDb, str, true).execute();
                        new SearchWordAsyncTask(ChatHeadService.this, webView2, oxfordHachetteDb, str, false).execute();
                    }

                    // hide the keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(edt.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        // main view - search image
        final ImageView searchImg = (ImageView) mainView.findViewById(R.id.imageView_search);
        searchImg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = edt.getText().toString();
                if (str.length() > 0) {
                    new SearchWordAsyncTask(ChatHeadService.this, webView, wiktionaryDb, str, true).execute();
                    new SearchWordAsyncTask(ChatHeadService.this, webView2, oxfordHachetteDb, str, false).execute();
                }

                // hide the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edt.getWindowToken(), 0);
            }
        });

        // main view - invisible top section
        View top = (View) mainView.findViewById(R.id.dialog_top);
        top.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mainView.setVisibility(View.INVISIBLE);
                mainViewVisible = false;
            }
        });

        // chathead
        chatheadView = (RelativeLayout) inflater.inflate(R.layout.chathead, null);
        windowManager.getDefaultDisplay().getSize(szWindow);
        WindowManager.LayoutParams chatheadParams = new WindowManager.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        chatheadParams.gravity = Gravity.TOP | Gravity.START;
        chatheadParams.x = 0;
        chatheadParams.y = 100;
        windowManager.addView(chatheadView, chatheadParams);

        chatheadView.setOnTouchListener(new ChatheadOnTouchListener(this));
        chatheadView.bringToFront();

        // main view - dictionary tabs
        TextView wiktionaryTv = (TextView) mainView.findViewById(R.id.textViewLabelDict1);
        TextView oxfordTv = (TextView) mainView.findViewById(R.id.textViewLabelDict2);
        final View wiktionaryIndicator = mainView.findViewById(R.id.indicatorDict1);
        final View oxfordIndicator = mainView.findViewById(R.id.indicatorDict2);
        final ScrollView scrollView1 = (ScrollView) mainView.findViewById(R.id.scrollView1);
        final ScrollView scrollView2 = (ScrollView) mainView.findViewById(R.id.scrollView2);
        OnClickListener dictTabOnClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView) v;
                if (tv.getText().equals("Wiktionary")) {
                    wiktionaryIndicator.setVisibility(View.VISIBLE);
                    oxfordIndicator.setVisibility(View.INVISIBLE);
                    scrollView1.setVisibility(View.VISIBLE);
                    scrollView2.setVisibility(View.GONE);
                }
                else if (tv.getText().equals("Oxford Hachette")) {
                    wiktionaryIndicator.setVisibility(View.INVISIBLE);
                    oxfordIndicator.setVisibility(View.VISIBLE);
                    scrollView1.setVisibility(View.GONE);
                    scrollView2.setVisibility(View.VISIBLE);
                }
            }
        };
        wiktionaryTv.setOnClickListener(dictTabOnClickListener);
        oxfordTv.setOnClickListener(dictTabOnClickListener);

        // automatically search word when copy to clipboard
        clipMan = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        regPrimaryClipChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        windowManager.getDefaultDisplay().getSize(szWindow);
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(Utility.LogTag, "ChatHeadService.onConfigurationChanged -> landscap");

            if (layoutParams.y + (chatheadView.getHeight() + getStatusBarHeight()) > szWindow.y) {
                layoutParams.y = szWindow.y - (chatheadView.getHeight() + getStatusBarHeight());
                windowManager.updateViewLayout(chatheadView, layoutParams);
            }

            if (layoutParams.x != 0 && layoutParams.x < szWindow.x) {
                resetPosition(szWindow.x);
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(Utility.LogTag, "ChatHeadService.onConfigurationChanged -> portrait");

            if (layoutParams.x > szWindow.x) {
                resetPosition(szWindow.x);
            }
        }
    }

    public void resetPosition(int x_cord_now) {
        int w = chatheadView.getWidth();

        if (x_cord_now == 0 || x_cord_now == szWindow.x - w) {

        } else if (x_cord_now + w / 2 <= szWindow.x / 2) {
            moveToLeft(x_cord_now);
        } else if (x_cord_now + w / 2 > szWindow.x / 2) {
            moveToRight(x_cord_now);
        }
    }

    public void moveToLeft(int x_cord_now) {
        final int x = x_cord_now;
        new CountDownTimer(500, 5) {
            WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();

            @Override
            public void onTick(long t) {
                long step = (500 - t) / 5;
                mParams.x = (int) (double) bounceValue(step, x);
                windowManager.updateViewLayout(chatheadView, mParams);
            }

            @Override
            public void onFinish() {
                mParams.x = 0;
                windowManager.updateViewLayout(chatheadView, mParams);
            }
        }.start();
    }

    public void moveToRight(int x_cord_now) {
        final int x = x_cord_now;
        new CountDownTimer(500, 5) {
            WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();

            @Override
            public void onTick(long t) {
                long step = (500 - t) / 5;
                mParams.x = szWindow.x + (int) (double) bounceValue(step, x) - chatheadView.getWidth();
                windowManager.updateViewLayout(chatheadView, mParams);
            }

            @Override
            public void onFinish() {
                mParams.x = szWindow.x - chatheadView.getWidth();
                windowManager.updateViewLayout(chatheadView, mParams);
            }
        }.start();
    }

    private double bounceValue(long step, long scale) {
        double value = scale * java.lang.Math.exp(-0.055 * step) * java.lang.Math.cos(0.08 * step);
        return value;
    }

    public int getStatusBarHeight() {
        int statusBarHeight = (int) Math.ceil(25 * getApplicationContext().getResources().getDisplayMetrics().density);
        return statusBarHeight;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Utility.LogTag, "ChatHeadService.onStartCommand()");
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    /**
     * Register the clipboard event handler
     */
    private void regPrimaryClipChanged() {
        if (!hasClipChangedListener) {
            clipMan.addPrimaryClipChangedListener(primaryClipChangedListener);
            hasClipChangedListener = true;
        }
    }

    /**
     * Unregister the clipboard event handler
     */
    private void unRegPrimaryClipChanged() {
        if (hasClipChangedListener) {
            clipMan.removePrimaryClipChangedListener(primaryClipChangedListener);
            hasClipChangedListener = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(Utility.LogTag, "ChatHeadService.onDestroy()");
        if (chatheadView != null) {
            windowManager.removeView(chatheadView);
        }

        if (removeView != null) {
            windowManager.removeView(removeView);
        }

        unRegPrimaryClipChanged();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Utility.LogTag, "ChatHeadService.onBind()");
        return null;
    }
}
