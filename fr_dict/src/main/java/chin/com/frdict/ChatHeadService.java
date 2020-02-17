package chin.com.frdict;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.List;

import chin.com.frdict.database.OxfordHachetteSqliteDatabase;
import chin.com.frdict.database.WiktionarySqliteDatabase;

/**
 * Main service for the app. Runs as a foreground service.
 */
public class ChatHeadService extends Service {

    private static final String NOTIFICATION_CHANNEL_ID = "chin.com.frdict";
    public static final String INTENT_FROM_CLIPBOARD = "FromClipboard";

    public static final String ACTION_TOGGLE_OPEN = "ACTION_TOGGLE_OPEN";
    public static final String ACTION_DISMISS = "ACTION_DISMISS";
    public static final String ACTION_SETTING = "ACTION_SETTING";

    @SuppressLint("StaticFieldLeak")
    public static ChatHeadService INSTANCE;

    private WindowManager windowManager;
    private Point szWindow = new Point();
    private RelativeLayout chatheadView;
    private RelativeLayout removeView;
    private ImageView removeImg;

    private ClipboardManager clipboardManager;
    private static boolean hasClipChangedListener = false;

    // dictionaries
    private WiktionarySqliteDatabase wiktionaryDb;
    private OxfordHachetteSqliteDatabase oxfordHachetteDb;

    // word list
    private AccentInsensitiveFilterArrayAdapter adapter;

    private BroadcastReceiver receiver;

    // time to create the adapter, in ms, for benchmarking purposes
    private long createAdapterTime;

    private SearchManager searchManager;

    /**
     * Event handler for looking up the word that was just copied into the clipboard
     */
    private ClipboardManager.OnPrimaryClipChangedListener primaryClipChangedListener = new FrDictPrimaryClipChangedListener(this);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Utility.LogTag, "ChatHeadService.onCreate()");
        INSTANCE = this;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        wiktionaryDb = WiktionarySqliteDatabase.getInstance(this);
        oxfordHachetteDb = OxfordHachetteSqliteDatabase.getInstance(this);

        searchManager = new SearchManager(this, wiktionaryDb, oxfordHachetteDb);

        printDbTableList();

        if (adapter == null) {
            Toast.makeText(ChatHeadService.this, "AutoCompleteTextView is initializing", Toast.LENGTH_SHORT).show();
            AsyncTask<Void, Void, Void> asyncTask = new PopulateWordListAsyncTask();
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // the remove and chathead views
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean chatheadEnabled = prefs.getBoolean(getString(R.string.pref_enableChathead), false);
        if (chatheadEnabled) {
            createChathead();
        }

        // automatically search word when copy to clipboard
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        regPrimaryClipChanged();

        // add the notification and actions
        // not sure if this is the correct way for adding new actions...

        Intent toggleOpenIntent = new Intent(ACTION_TOGGLE_OPEN);
        PendingIntent piToggleOpen = PendingIntent.getBroadcast(this, 0, toggleOpenIntent, 0);

        Intent dismissIntent = new Intent(ACTION_DISMISS);
        dismissIntent.setAction(ACTION_DISMISS);
        PendingIntent piDismiss = PendingIntent.getBroadcast(this, 0, dismissIntent, 0);

        Intent settingIntent = new Intent(ACTION_SETTING);
        settingIntent.setAction(ACTION_SETTING);
        PendingIntent piSetting = PendingIntent.getBroadcast(this, 0, settingIntent, 0);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TOGGLE_OPEN);
        filter.addAction(ACTION_DISMISS);
        filter.addAction(ACTION_SETTING);

        receiver = new FrDictBroadcastReceiver();
        registerReceiver(receiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startNewStyleForeground(piToggleOpen, piDismiss, piSetting);
        }
        else {
            startOldStyleForeground(piToggleOpen, piDismiss, piSetting);
        }
    }

    private void createChathead() {
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        else {
            type = LayoutParams.TYPE_PHONE;
        }
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        addRemoveView(type, inflater);
        addChatHeadView(type, inflater);
    }

    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    private void addChatHeadView(int type, LayoutInflater inflater) {
        // Note: DO NOT add FLAG_NOT_FOCUSABLE here, as we need to be focusable to receive
        // clipboard events in Android 10+. Also need to specify FLAG_NOT_TOUCH_MODAL,
        // otherwise the view will capture all touch events even those outside the chathead view
        int flags = LayoutParams.FLAG_NOT_TOUCH_MODAL |
                LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        chatheadView = (RelativeLayout) inflater.inflate(R.layout.chathead, null);
        windowManager.getDefaultDisplay().getSize(szWindow);
        LayoutParams chatheadParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                type, flags, PixelFormat.TRANSLUCENT);
        chatheadParams.gravity = Gravity.TOP | Gravity.START;
        chatheadParams.x = 0;
        chatheadParams.y = 100;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int chatheadSize = prefs.getInt(getString(R.string.pref_chatheadSize), 150);

        chatheadParams.width = chatheadSize;
        chatheadParams.height = chatheadSize;
        windowManager.addView(chatheadView, chatheadParams);

        chatheadView.setOnTouchListener(new ChatheadOnTouchListener(this));
    }

    @SuppressLint("InflateParams")
    private void addRemoveView(int type, LayoutInflater inflater) {
        removeView = (RelativeLayout) inflater.inflate(R.layout.remove, null);

        // Note that FLAG_NOT_FOCUSABLE also implies FLAG_NOT_TOUCH_MODAL
        int flags = LayoutParams.FLAG_NOT_FOCUSABLE |
                LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        LayoutParams paramRemove = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                type, flags, PixelFormat.TRANSLUCENT);
        paramRemove.gravity = Gravity.TOP | Gravity.START;
        removeView.setVisibility(View.GONE);
        removeImg = removeView.findViewById(R.id.remove_img);
        windowManager.addView(removeView, paramRemove);
    }

    private void startOldStyleForeground(PendingIntent piToggleOpen, PendingIntent piDismiss, PendingIntent piSetting) {
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.circle)
                .setContentTitle("frdict is running")
                .setContentText("Click to show/hide the dictionary")
                .setContentIntent(piToggleOpen)
                .addAction(android.R.drawable.ic_menu_preferences, "Settings", piSetting)
                .addAction(R.drawable.ic_stat_dismiss, "Dismiss", piDismiss)
                .build();

        startForeground(1337, notification);
    }

    /**
     * Need to create a NotificationChannel to be able to run a foreground service in new Android versions
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startNewStyleForeground(PendingIntent piToggleOpen, PendingIntent piDismiss, PendingIntent piSetting){
        String channelName = "frdict background service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.circle)
                .setContentTitle("frdict is running")
                .setContentText("Click to show/hide the dictionary")
                .setContentIntent(piToggleOpen)
                .addAction(android.R.drawable.ic_menu_preferences, "Settings", piSetting)
                .addAction(R.drawable.ic_stat_dismiss, "Dismiss", piDismiss)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1337, notification);
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
            clipboardManager.addPrimaryClipChangedListener(primaryClipChangedListener);
            hasClipChangedListener = true;
        }
    }

    /**
     * Unregister the clipboard event handler
     */
    private void unRegPrimaryClipChanged() {
        if (hasClipChangedListener) {
            clipboardManager.removePrimaryClipChangedListener(primaryClipChangedListener);
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
        unregisterReceiver(receiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Utility.LogTag, "ChatHeadService.onBind()");
        return null;
    }

    public AccentInsensitiveFilterArrayAdapter getAdapter() {
        return adapter;
    }

    public SearchManager getSearchManager() {
        return searchManager;
    }

    public WiktionarySqliteDatabase getWiktionaryDb() {
        return wiktionaryDb;
    }

    public OxfordHachetteSqliteDatabase getOxfordHachetteDb() {
        return oxfordHachetteDb;
    }

    public long getCreateAdapterTime() {
        return createAdapterTime;
    }

    public RelativeLayout getChatheadView() {
        return chatheadView;
    }

    public RelativeLayout getRemoveView() {
        return removeView;
    }

    public ImageView getRemoveImg() {
        return removeImg;
    }

    private void printDbTableList() {
        List<String> wiktionaryDbTables = wiktionaryDb.getTableList();
        StringBuilder sb = new StringBuilder();
        for (String s : wiktionaryDbTables) {
            sb.append(s).append(",");
        }
        sb.setCharAt(sb.length() - 1, ']');
        Log.i("frdict", "wiktionary db table list: [" + sb.toString());

        List<String> oxfordDbTables = oxfordHachetteDb.getTableList();
        sb = new StringBuilder();
        for (String s : oxfordDbTables) {
            sb.append(s).append(",");
        }
        sb.setCharAt(sb.length() - 1, ']');
        Log.i("frdict", "oxford hachette db table list: [" + sb.toString());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        windowManager.getDefaultDisplay().getSize(szWindow);
        LayoutParams layoutParams = (LayoutParams) chatheadView.getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(Utility.LogTag, "ChatHeadService.onConfigurationChanged -> landscape");

            if (layoutParams.y + (chatheadView.getHeight() + getStatusBarHeight()) > szWindow.y) {
                layoutParams.y = szWindow.y - (chatheadView.getHeight() + getStatusBarHeight());
                windowManager.updateViewLayout(chatheadView, layoutParams);
            }

            if (layoutParams.x != 0 && layoutParams.x < szWindow.x) {
                resetPosition(szWindow.x);
            }
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(Utility.LogTag, "ChatHeadService.onConfigurationChanged -> portrait");

            if (layoutParams.x > szWindow.x) {
                resetPosition(szWindow.x);
            }
        }
    }

    public void resetPosition(int xCordNow) {
        int w = chatheadView.getWidth();

        if (xCordNow == 0 || xCordNow == szWindow.x - w) {
            // do nothing
        }
        else if (xCordNow + w / 2 <= szWindow.x / 2) {
            moveToLeft(xCordNow);
        }
        else if (xCordNow + w / 2 > szWindow.x / 2) {
            moveToRight(xCordNow);
        }
    }

    public void moveToLeft(int xCordNow) {
        final int x = xCordNow;
        new CountDownTimer(500, 5) {
            LayoutParams mParams = (LayoutParams) chatheadView.getLayoutParams();

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

    public void moveToRight(int xCordNow) {
        final int x = xCordNow;
        new CountDownTimer(500, 5) {
            LayoutParams mParams = (LayoutParams) chatheadView.getLayoutParams();

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
        return scale * Math.exp(-0.055 * step) * Math.cos(0.08 * step);
    }

    public int getStatusBarHeight() {
        return (int) Math.ceil(25 * getApplicationContext().getResources().getDisplayMetrics().density);
    }

    public Point getSzWindow() {
        return szWindow;
    }

    public WindowManager getWindowManager() {
        return windowManager;
    }

    public ClipboardManager getClipboardManager() {
        return clipboardManager;
    }

    @SuppressLint("StaticFieldLeak")
    private class PopulateWordListAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Log.i("frdict", "Start getting word list");
            List<String> wordList = wiktionaryDb.getWordList();
            Log.i("frdict", "End getting word list, start creating adapter");

            long start = System.currentTimeMillis();
            List<String> accentRemovedList = wiktionaryDb.getNoAccentWordList();
            long end = System.currentTimeMillis();
            createAdapterTime = (end - start);
            Log.i("frdict", "AccentInsensitiveFilterArrayAdapter - creating accentRemovedList time: " + createAdapterTime + "ms");

            adapter = new AccentInsensitiveFilterArrayAdapter(ChatHeadService.this, R.layout.autocomplete_dropdown_item, wordList, accentRemovedList);
            Log.i("frdict", "End creating adapter");
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            Toast.makeText(ChatHeadService.this, "AutoCompleteTextView is now ready", Toast.LENGTH_SHORT).show();
        }
    }
}
