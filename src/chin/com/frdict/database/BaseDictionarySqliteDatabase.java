package chin.com.frdict.database;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BaseDictionarySqliteDatabase extends SQLiteAssetHelper{
    protected SQLiteDatabase db;
    protected static Context context;

    protected BaseDictionarySqliteDatabase(Context context, String dbName, int dbVersion) {
        super(context, dbName, null, dbVersion);
        setForcedUpgrade();
        BaseDictionarySqliteDatabase.context = context;
    }

    public SQLiteDatabase getBackendDatabase() {
        return db;
    }

    public String getWordDefinition(String name) {
        String definition;
        try {
            Cursor cursor = db.rawQuery("select definition from word where name = ? collate nocase", new String[] { name });

            if (cursor.getCount() == 0) {
                return "Word not found: " + name;
            }

            cursor.moveToFirst();
            definition = cursor.getString(cursor.getColumnIndex("definition"));
        }
        catch (Exception e) {
            definition = "Something went wrong when querying offline database.";
            e.printStackTrace();
        }

        return definition;
    }
}
