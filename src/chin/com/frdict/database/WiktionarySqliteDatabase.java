package chin.com.frdict.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

/**
 * Helper class for working with the Wiktionary database
 * @author Chin
 */
public class WiktionarySqliteDatabase extends BaseDictionarySqliteDatabase {
    private static final String DATABASE_NAME = Environment.getExternalStorageDirectory().getPath() + "/frdicts/wiktionary_fren.db";
    public static final int DATABASE_VERSION = 20151114;
    protected static BaseDictionarySqliteDatabase instance;

    private WiktionarySqliteDatabase(Context context) {
        super(context, DATABASE_NAME, DATABASE_VERSION);
    }

    public static BaseDictionarySqliteDatabase getInstance(Context context) {
        if (instance == null) {
            WiktionarySqliteDatabase dbHelper = new WiktionarySqliteDatabase(context);
            dbHelper.db = SQLiteDatabase.openDatabase(DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            instance = dbHelper;
        }
        return instance;
    }
}