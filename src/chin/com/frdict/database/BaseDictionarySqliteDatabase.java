package chin.com.frdict.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class BaseDictionarySqliteDatabase {
    protected SQLiteDatabase db;
    protected static Context context;

    protected BaseDictionarySqliteDatabase(Context context, String dbName, int dbVersion) {
        BaseDictionarySqliteDatabase.context = context;
    }

    public SQLiteDatabase getBackendDatabase() {
        return db;
    }

    public String getWordDefinition(String name) {
        String definition = null;
        try {
            Cursor cursor = db.rawQuery("select definition from word where name = ? collate nocase", new String[] { name });

            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            definition = cursor.getString(cursor.getColumnIndex("definition"));
        }
        catch (Exception e) {
            Log.e("frdict", "Something went wrong when querying offline database.");
            e.printStackTrace();
        }

        return definition;
    }
}
