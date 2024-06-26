package chin.com.frdict;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import chin.com.frdict.activity.DictionaryActivity;
import chin.com.frdict.activity.SettingsActivity;

class FrDictBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Just to shut up Android Studio, but can this ever be true?
        if (action == null) {
            return;
        }

        switch (action) {
            case ChatHeadService.ACTION_TOGGLE_OPEN:
                if (DictionaryActivity.active) {
                    DictionaryActivity.INSTANCE.moveTaskToBack(true);
                } else {
                    Intent it = new Intent(ChatHeadService.INSTANCE, DictionaryActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    ChatHeadService.INSTANCE.startActivity(it);
                }
                break;
            case ChatHeadService.ACTION_SETTING:
                Intent it = new Intent(ChatHeadService.INSTANCE, SettingsActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ChatHeadService.INSTANCE.startActivity(it);

                // close the notification drawer
                it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(it);
                break;
            case ChatHeadService.ACTION_DISMISS:
                ChatHeadService.INSTANCE.exit();
                break;
        }
    }
}
