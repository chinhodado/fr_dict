package chin.com.frdict.database;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class OxfordHachetteSqliteDatabase extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "oxford hachette v3.db";
    public static final int DATABASE_VERSION = 20151206;
    private static SQLiteDatabase db;
    static Context context;

    private OxfordHachetteSqliteDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgrade();
        OxfordHachetteSqliteDatabase.context = context;
    }

    public static void InitializeDatabase(Context context) {
        if (db == null) {
            OxfordHachetteSqliteDatabase dbHelper = new OxfordHachetteSqliteDatabase(context);
            db = dbHelper.getReadableDatabase();
        }
        else {
            Log.i("OxfordHachetteSqliteDatabase", "Already initialized");
        }
    }

    public static SQLiteDatabase getDatabase() {
        return db;
    }
}
