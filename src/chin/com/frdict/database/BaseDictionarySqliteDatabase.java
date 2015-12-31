package chin.com.frdict.database;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
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

    public List<String> getWordList() {
        // here's hoping our database doesn't have too much rows that the count won't fit into an int...
        int numEntries = (int)DatabaseUtils.queryNumEntries(db, "word");
        ArrayList<String> wordList = new ArrayList<String>(numEntries);
        try {
            Cursor cursor = db.rawQuery("select name from word order by name", null);

            if (cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    String name = cursor.getString(cursor.getColumnIndex("name"));
                    wordList.add(name);
                    cursor.moveToNext();
                }
            }
        }
        catch (Exception e) {
            Log.e("frdict", "Error getting word list.");
            e.printStackTrace();
        }
        return wordList;
    }
}
