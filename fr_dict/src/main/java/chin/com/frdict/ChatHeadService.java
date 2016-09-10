package chin.com.frdict;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.List;

import chin.com.frdict.activity.DictionaryActivity;
import chin.com.frdict.database.BaseDictionarySqliteDatabase;
import chin.com.frdict.database.OxfordHachetteSqliteDatabase;
import chin.com.frdict.database.WiktionarySqliteDatabase;

public class ChatHeadService extends Service {
    public static WindowManager windowManager;
    public Point szWindow = new Point();
    public static ChatHeadService instance;
    ClipboardManager clipMan;
    static boolean hasClipChangedListener = false;

    // dictionaries
    public static BaseDictionarySqliteDatabase wiktionaryDb;
    public static BaseDictionarySqliteDatabase oxfordHachetteDb;

    // word list
    public static AccentInsensitiveFilterArrayAdapter adapter;

    public static final String INTENT_FROM_CLIPBOARD = "FromClipboard";

    BroadcastReceiver receiver;

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
                // execute SearchWordAsyncTask ourselves, or let MyDialog do it, depending whether it is active or not
                if (!DictionaryActivity.active) {
                    Intent intent = new Intent(ChatHeadService.this, DictionaryActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(INTENT_FROM_CLIPBOARD, str);
                    startActivity(intent);
                }
                else {
                    ChatHeadService.searchWord(str);
                    DictionaryActivity.instance.edt.setText(str);
                }
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
        ChatHeadService.wiktionaryDb = WiktionarySqliteDatabase.getInstance(this);
        ChatHeadService.oxfordHachetteDb = OxfordHachetteSqliteDatabase.getInstance(this);

        if (adapter == null) {
            Toast.makeText(ChatHeadService.this, "AutoCompleteTextView is initializing", Toast.LENGTH_SHORT).show();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Log.i("frdict", "Start getting word list");
                    List<String> wordList = wiktionaryDb.getWordList();
                    Log.i("frdict", "End getting word list, start creating adapter");

                    long start = System.currentTimeMillis();
                    List<String> accentRemovedList = ChatHeadService.wiktionaryDb.getNoAccentWordList();
                    long end = System.currentTimeMillis();
                    long duration = (end - start);
                    Log.i("frdict", "AccentInsensitiveFilterArrayAdapter - creating accentRemovedList time: " + duration + "ms");

                    adapter = new AccentInsensitiveFilterArrayAdapter(ChatHeadService.this, R.layout.autocomplete_dropdown_item, wordList, accentRemovedList);
                    Log.i("frdict", "End creating adapter");
                    return null;
                }

                @Override
                protected void onPostExecute(Void param) {
                    Toast.makeText(ChatHeadService.this, "AutoCompleteTextView is now ready", Toast.LENGTH_SHORT).show();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // automatically search word when copy to clipboard
        clipMan = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        regPrimaryClipChanged();

        // add the notification and actions
        // not sure if this is the correct way for adding new actions...
        final String actionToogleOpen = "ACTION_TOOGLE_OPEN";
        final String actionDismiss = "ACTION_DISMISS";

        Intent toogleOpenIntent = new Intent(actionToogleOpen);
        PendingIntent piToogleOpen = PendingIntent.getBroadcast(this, 0, toogleOpenIntent, 0);

        Intent dismissIntent = new Intent(actionDismiss);
        dismissIntent.setAction(actionDismiss);
        PendingIntent piDismiss = PendingIntent.getBroadcast(this, 0, dismissIntent, 0);

        IntentFilter filter = new IntentFilter();
        filter.addAction(actionToogleOpen);
        filter.addAction(actionDismiss);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(actionToogleOpen)) {
                    if (DictionaryActivity.active) {
                        DictionaryActivity.instance.moveTaskToBack(true);
                    } else {
                        Intent it = new Intent(ChatHeadService.instance, DictionaryActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        ChatHeadService.instance.startActivity(it);
                    }
                }
                else if (action.equals(actionDismiss)) {
                    if (DictionaryActivity.instance != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            DictionaryActivity.instance.finishAndRemoveTask();
                        }
                        else {
                            // This will leave the task in the task list
                            // I'm too lazy to figure out how to do this (remove the task) properly on lower APIs
                            // and I don't own any pre-lollipop device anyway...
                            DictionaryActivity.instance.finish();
                        }
                    }

                    ChatHeadService.instance.stopSelf();
                }
            }
        };

        registerReceiver(receiver, filter);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.circle)
                .setContentTitle("frdict is running")
                .setContentText("Click to show/hide the dictionary")
                .setContentIntent(piToogleOpen)
                .addAction(R.drawable.ic_stat_dismiss, "Dismiss", piDismiss)
                .build();

        startForeground(1337, notification);
    }

    public static void searchWord(String word) {
        new SearchWordAsyncTask(ChatHeadService.instance, DictionaryActivity.webViewWiktionary, ChatHeadService.wiktionaryDb, word).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new SearchWordAsyncTask(ChatHeadService.instance, DictionaryActivity.webViewOxfordHachette, ChatHeadService.oxfordHachetteDb, word).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
}
