package chin.com.frdict.database;

import android.content.Context;

/**
 * Helper class for working with the Wiktionary database
 * @author Chin
 */
public class WiktionarySqliteDatabase extends BaseDictionarySqliteDatabase {
    private static final String DATABASE_NAME = "wiktionary_fren.db";
    public static final int DATABASE_VERSION = 20151114;
    protected static BaseDictionarySqliteDatabase instance;

    private WiktionarySqliteDatabase(Context context) {
        super(context, DATABASE_NAME, DATABASE_VERSION);
    }

    public static BaseDictionarySqliteDatabase getInstance(Context context) {
        if (instance == null) {
            WiktionarySqliteDatabase dbHelper = new WiktionarySqliteDatabase(context);
            dbHelper.db = dbHelper.getReadableDatabase();
            instance = dbHelper;
        }
        return instance;
    }
}