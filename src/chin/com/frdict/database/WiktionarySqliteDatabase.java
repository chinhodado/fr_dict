package chin.com.frdict.database;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.Toast;

/**
 * Helper class for working with the Wiktionary database
 * @author Chin
 */
public class WiktionarySqliteDatabase extends BaseDictionarySqliteDatabase {
    private static final String DATABASE_NAME = Environment.getExternalStorageDirectory().getPath() + "/frdicts/wiktionary_fren.db";
    private static final String DATABASE_NAME_2 = Environment.getExternalStorageDirectory().getPath() + "/Android/obb/frdicts/wiktionary_fren.db";
    public static final int DATABASE_VERSION = 20151114;
    protected static BaseDictionarySqliteDatabase instance;

    private WiktionarySqliteDatabase(Context context) {
        super(context, "Wiktionary FR-EN");
    }

    public static BaseDictionarySqliteDatabase getInstance(Context context) {
        if (instance == null) {
            WiktionarySqliteDatabase dbHelper = new WiktionarySqliteDatabase(context);
            File file = new File(DATABASE_NAME);
            File file2 = new File(DATABASE_NAME_2);
            if (file.exists()) {
                dbHelper.db = SQLiteDatabase.openDatabase(DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            }
            else if (file2.exists()) {
                dbHelper.db = SQLiteDatabase.openDatabase(DATABASE_NAME_2, null, SQLiteDatabase.OPEN_READONLY);
            }
            else {
                Toast.makeText(context, "wiktionary_fren.db not found", Toast.LENGTH_LONG).show();
                System.exit(0);
            }
            instance = dbHelper;
        }
        return instance;
    }

    @Override
    public String getWordDefinition(String name) {
        String definition = super.getWordDefinition(name);
        if (definition != null) {
            String css = "<style> a { color: rgba(54, 95, 145, 1); } </style>";
            // this is just a heuristic and won't be accurate in all cases
            definition = definition.replaceAll("<i>(\\w+)</i>", "<a href='frdict://search\\?word=$1'><i>$1</i></a>");
            definition = css + definition;
        }
        return definition;
    }
}