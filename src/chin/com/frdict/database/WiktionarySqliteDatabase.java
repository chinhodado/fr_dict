package chin.com.frdict.database;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Helper class for database provisioning
 * @author Chin
 *
 */
public class WiktionarySqliteDatabase extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "wiktionary_fren.db";
    public static final int DATABASE_VERSION = 20151114;
    private static SQLiteDatabase db;
    static Context context;

    private WiktionarySqliteDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgrade();
        WiktionarySqliteDatabase.context = context;
    }

    public static void InitializeDatabase(Context context) {
        if (db == null) {
            WiktionarySqliteDatabase dbHelper = new WiktionarySqliteDatabase(context);
            db = dbHelper.getReadableDatabase();
        }
        else {
            Log.i("WiktionarySqliteDatabase", "Already initialized");
        }
    }

    public static SQLiteDatabase getDatabase() {
        return db;
    }
}