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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.Objects;

import chin.com.frdict.activity.DictionaryActivity;
import chin.com.frdict.activity.SettingsActivity;
import chin.com.frdict.database.OxfordHachetteSqliteDatabase;
import chin.com.frdict.database.WiktionarySqliteDatabase;

/**
 * Main service for the app. Runs as a foreground service.
 */
public class ChatHeadService extends Service {

    private static final String NOTIFICATION_CHANNEL_ID = "chin.com.frdict";
    public static final String INTENT_FROM_CLIPBOARD = "FromClipboard";

    private static final String ACTION_TOGGLE_OPEN = "ACTION_TOGGLE_OPEN";
    private static final String ACTION_DISMISS = "ACTION_DISMISS";
    private static final String ACTION_SETTING = "ACTION_SETTING";

    public static WindowManager windowManager;
    public static ChatHeadService INSTANCE;
    private ClipboardManager clipMan;
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
    ClipboardManager.OnPrimaryClipChangedListener primaryClipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
        private static final long THRESHOLD_MS = 50;
        private long lastChangedTime = 0;
        private String lastString = "";
        @Override
        public void onPrimaryClipChanged() {
            try {
                String str = clipMan.getText().toString();

                // Copying text from certain places will trigger multiple events (e.g. Chrome/WebView generates 3 events)
                // Ignore the duplicated events
                if (System.currentTimeMillis() - lastChangedTime < THRESHOLD_MS && Objects.equals(lastString, str)) {
                    return;
                }

                lastChangedTime = System.currentTimeMillis();
                lastString = str;

                if (str != null && str.trim().length() > 0) {
                    str = str.trim();

                    // don't react to links
                    if (str.startsWith("http")) {
                        return;
                    }

                    // ignore anything that contains a number
                    if (str.matches(".*\\d+.*")) {
                        return;
                    }

                    // trim , . ; at the end
                    str = str.replaceAll("([,.;])+$", "");

                    // trim , . ; at the beginning
                    str = str.replaceAll("^([,.;])+", "");

                    // deal with "words" like t'aime, m'appelle, s'occuper, etc.
                    char second = str.charAt(1);
                    if (second == '\'' || second == '’') {
                        str = str.substring(2);
                    }

                    str = str.toLowerCase();

                    // execute SearchWordAsyncTask ourselves, or let MyDialog do it, depending whether it is active or not
                    if (!DictionaryActivity.active) {
                        // TODO: why do we need to do this?
                        searchManager.cancelTasks();
                        Intent intent = new Intent(ChatHeadService.this, DictionaryActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra(INTENT_FROM_CLIPBOARD, str);
                        startActivity(intent);
                    }
                    else {
                        searchManager.searchWord(str);
                        DictionaryActivity.INSTANCE.edt.setText(str);
                    }
                }
            }
            catch (Exception e) {
                Toast.makeText(getApplicationContext(), "frdict: An error occurred", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    };

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

        // automatically search word when copy to clipboard
        clipMan = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startNewStyleForeground(piToggleOpen, piDismiss, piSetting);
        else {
            startOldStyleForeground(piToggleOpen, piDismiss, piSetting);
        }
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

    private class FrDictBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Just to shut up Android Studio, but can this ever be true?
            if (action == null) {
                return;
            }

            switch (action) {
                case ACTION_TOGGLE_OPEN:
                    if (DictionaryActivity.active) {
                        DictionaryActivity.INSTANCE.moveTaskToBack(true);
                    } else {
                        Intent it = new Intent(ChatHeadService.INSTANCE, DictionaryActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        ChatHeadService.INSTANCE.startActivity(it);
                    }
                    break;
                case ACTION_SETTING:
                    Intent it = new Intent(ChatHeadService.INSTANCE, SettingsActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ChatHeadService.INSTANCE.startActivity(it);

                    // close the notification drawer
                    it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    context.sendBroadcast(it);
                    break;
                case ACTION_DISMISS:
                    if (DictionaryActivity.INSTANCE != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            DictionaryActivity.INSTANCE.finishAndRemoveTask();
                        } else {
                            // This will leave the task in the task list
                            // I'm too lazy to figure out how to do this (remove the task) properly on lower APIs
                            // and I don't own any pre-lollipop device anyway...
                            DictionaryActivity.INSTANCE.finish();
                        }
                    }

                    ChatHeadService.INSTANCE.stopSelf();
                    break;
            }
        }
    }
}
