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
    public String dictName;

    protected BaseDictionarySqliteDatabase(Context context, String dictName) {
        BaseDictionarySqliteDatabase.context = context;
        this.dictName = dictName;
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
        long start = System.currentTimeMillis();
        // here's hoping our database doesn't have too much rows that the count won't fit into an int...
        int numEntries = (int)DatabaseUtils.queryNumEntries(db, "word");
        ArrayList<String> wordList = new ArrayList<String>(numEntries);
        try {
            // Split the query into chunks of 10000 each. This avoids the "CursorWindow Window is full" warning and is
            // a lot faster than getting everything in one go (about three times faster)
            int chunkSize = 10000;
            int iterations = (int) Math.ceil((double)numEntries / chunkSize);
            for (int i = 0; i < iterations; i++) {
                Cursor cursor = db.rawQuery("select name from word order by name LIMIT " + chunkSize + " OFFSET " + chunkSize * i, null);
                if (cursor.moveToFirst()) {
                    int nameColumnIndex = cursor.getColumnIndex("name");
                    while (cursor.isAfterLast() == false) {
                        String name = cursor.getString(nameColumnIndex);
                        wordList.add(name);
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
        }
        catch (Exception e) {
            Log.e("frdict", "Error getting word list.");
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        long duration = (end - start);
        Log.i("frdict", dictName + " - getting word list time: " + duration + "ms");
        Log.i("frdict", dictName + " - num entries: " + numEntries);
        return wordList;
    }
}
