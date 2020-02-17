package chin.com.frdict;

import android.content.ClipboardManager;
import android.content.Intent;
import android.widget.Toast;

import java.util.Objects;

import chin.com.frdict.activity.DictionaryActivity;

class FrDictPrimaryClipChangedListener implements ClipboardManager.OnPrimaryClipChangedListener {
    private static final long THRESHOLD_MS = 50;
    private ChatHeadService chatHeadService;
    private long lastChangedTime = 0;
    private String lastString = "";

    public FrDictPrimaryClipChangedListener(ChatHeadService chatHeadService) {
        this.chatHeadService = chatHeadService;
    }

    @Override
    public void onPrimaryClipChanged() {
        try {
            String str = chatHeadService.getClipboardManager().getText().toString();

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
                    chatHeadService.getSearchManager().cancelTasks();
                    Intent intent = new Intent(chatHeadService, DictionaryActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(ChatHeadService.INTENT_FROM_CLIPBOARD, str);
                    chatHeadService.startActivity(intent);
                }
                else {
                    chatHeadService.getSearchManager().searchWord(str);
                    DictionaryActivity.INSTANCE.edt.setText(str);
                }
            }
        }
        catch (Exception e) {
            Toast.makeText(chatHeadService.getApplicationContext(), "frdict: An error occurred", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
