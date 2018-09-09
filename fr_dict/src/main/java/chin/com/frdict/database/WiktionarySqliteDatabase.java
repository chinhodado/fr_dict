package chin.com.frdict.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import java.io.File;

/**
 * Helper class for working with the Wiktionary database
 * @author Chin
 */
public class WiktionarySqliteDatabase extends BaseDictionarySqliteDatabase {
    public static final int DATABASE_VERSION = 20151114;
    protected static WiktionarySqliteDatabase instance;

    private WiktionarySqliteDatabase(Context context) {
        super(context, "Wiktionary FR-EN", "wiktionary_fren.db");
    }

    public static WiktionarySqliteDatabase getInstance(Context context) {
        if (instance == null) {
            WiktionarySqliteDatabase dbHelper = new WiktionarySqliteDatabase(context);
            File file = new File(dbHelper.getDatabasePath());
            File file2 = new File(dbHelper.getDatabaseAlternatePath());
            if (file.exists() || file2.exists()) {
                // Note: I experimented with in-memory database but somehow the performance is the
                // same as on-disk database. Something must be wrong. Either SQLite already caches the
                // database into memory automatically, or my way of creating in-memory db is incorrect.
                // Anyway, I'm leaving the code for in-memory db below so I can come back in the future
                // if needed.
                //
                // Log.i("frdict", "Start creating in memory db for " + dbHelper.databaseFileName);
                // long startTime = System.currentTimeMillis();
                // SQLiteDatabase memDb = SQLiteDatabase.create(null);
                // memDb.execSQL("ATTACH DATABASE '" + path + "' AS other");
                // memDb.execSQL("CREATE TABLE word AS SELECT * FROM other.word");
                // long endTime = System.currentTimeMillis();
                // Log.i("frdict", "End creating in memory db for " + dbHelper.databaseFileName + ", time: " + (endTime-startTime) + "ms");
                // dbHelper.db = memDb;
                String path = file.exists()? dbHelper.getDatabasePath() : dbHelper.getDatabaseAlternatePath();
                dbHelper.db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
                dbHelper.createFtsTable();
            }
            else {
                Toast.makeText(context, dbHelper.databaseFileName + " not found", Toast.LENGTH_LONG).show();
                System.exit(0);
            }
            instance = dbHelper;
        }
        return instance;
    }
}