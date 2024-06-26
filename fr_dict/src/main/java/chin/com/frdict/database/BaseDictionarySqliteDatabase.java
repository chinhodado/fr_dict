package chin.com.frdict.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class BaseDictionarySqliteDatabase {
    protected SQLiteDatabase db;
    protected final Context context;
    protected final String dictName;
    protected final String databaseFileName;

    protected BaseDictionarySqliteDatabase(Context context, String dictName, String databaseFileName) {
        this.context = context;
        this.dictName = dictName;
        this.databaseFileName = databaseFileName;
    }

    public SQLiteDatabase getBackendDatabase() {
        return db;
    }

    public String getDatabasePath() {
        return Environment.getExternalStorageDirectory().getPath() + "/frdicts/" + databaseFileName;
    }

    public String getDatabaseAlternatePath() {
        return Environment.getExternalStorageDirectory().getPath() + "/Android/obb/frdicts/" + databaseFileName;
    }

    public String getCachePath() {
        String prefix = databaseFileName.split("\\.")[0];
        return Environment.getExternalStorageDirectory().getPath() + "/frdicts/" + prefix + "_allWords.txt";
    }

    public String getNoAccentCachePath() {
        String prefix = databaseFileName.split("\\.")[0];
        return Environment.getExternalStorageDirectory().getPath() + "/frdicts/" + prefix + "_allWordsNoAccent.txt";
    }

    public String getWordDefinition(String name) {
        long start = System.currentTimeMillis();
        String definition = null;
        try {
            Cursor cursor = db.rawQuery("select definition from word where name = ? collate nocase", new String[] { name });

            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            definition = cursor.getString(cursor.getColumnIndex("definition"));

            cursor.close();
        }
        catch (Exception e) {
            Log.e("frdict", dictName + " - Something went wrong when querying offline database.", e);
        }

        long end = System.currentTimeMillis();
        Log.i("frdict", dictName + " - Search for \"" + name + "\" took " + (end-start) + "ms");
        return definition;
    }

    public List<String> getDeepSearchResults(String toSearch, int limit) {
        long start = System.currentTimeMillis();

        List<String> results = new ArrayList<>();
        String limitClause = "";
        if (limit != 0) {
            limitClause = " limit " + limit;
        }
        try {
            Cursor cursor = db.rawQuery("select name from fts where definition match ? collate nocase" + limitClause,
                    new String[] { "\"" + toSearch + "\""});

            if (cursor.moveToFirst()) {
                int nameColumnIndex = cursor.getColumnIndex("name");
                while (!cursor.isAfterLast()) {
                    String name = cursor.getString(nameColumnIndex);
                    results.add(name);
                    cursor.moveToNext();
                }
            }

            cursor.close();
        }
        catch (Exception e) {
            Log.e("frdict", dictName + " - Something went wrong when querying offline database.", e);
        }
        long end = System.currentTimeMillis();
        Log.i("frdict", dictName + " - Deep search for \"" + toSearch + "\" took " + (end-start) + "ms");
        return results;
    }

    public String getDeepSearchResultsHtml(String toSearch, int limit) {
        List<String> results = getDeepSearchResults(toSearch, limit);
        StringBuilder sb = new StringBuilder();
        sb.append("Deep search result for " + toSearch + ": <br>");
        for (String s : results) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("frdict")
                    .appendPath("search")
                    .appendQueryParameter("word", s)
                    .appendQueryParameter("highlight", toSearch);
            String myUrl = builder.build().toString();
            sb.append("<a href=\"" + myUrl + "\">" + s + "</a><br>");
        }
        return sb.toString();
    }

    public List<String> getWordList() {
        long start = System.currentTimeMillis();
        // here's hoping our database doesn't have too many rows that the count won't fit into an int...
        int numEntries = (int)DatabaseUtils.queryNumEntries(db, "word");
        List<String> wordList = null;
        String allWords;
        try {
            File file = new File(getCachePath());
            if (file.exists()) {
                // Use the pre-built word list
                Log.i("frdict", dictName + " - allWords cache file found, using it");
                BufferedReader br = new BufferedReader(new FileReader(file));
                allWords = br.readLine();
                br.close();
                wordList = Arrays.asList(allWords.split(";"));
            }
            else {
                // Build the word list manually from the database. This is slow.

                // Split the query into chunks of 10000 each. This avoids the "CursorWindow Window is full" warning and is
                // a lot faster than getting everything in one go (about three times faster)
                Log.i("frdict", dictName + " - allWords cache file not found, building word list from scratch");
                wordList = new ArrayList<>(numEntries);
                int chunkSize = 10000;
                int iterations = (int) Math.ceil((double)numEntries / chunkSize);
                for (int i = 0; i < iterations; i++) {
                    Cursor cursor = db.rawQuery("select name from word order by name LIMIT " + chunkSize + " OFFSET " + chunkSize * i, null);
                    if (cursor.moveToFirst()) {
                        int nameColumnIndex = cursor.getColumnIndex("name");
                        while (!cursor.isAfterLast()) {
                            String name = cursor.getString(nameColumnIndex);
                            wordList.add(name);
                            cursor.moveToNext();
                        }
                    }
                    cursor.close();
                }
            }
        }
        catch (Exception e) {
            Log.e("frdict", dictName + " - Error getting word list.", e);
        }
        long end = System.currentTimeMillis();
        long duration = (end - start);
        Log.i("frdict", dictName + " - getting word list time: " + duration + "ms");
        Log.i("frdict", dictName + " - num entries: " + numEntries);
        return wordList;
    }

    public List<String> getNoAccentWordList() {
        List<String> wordList = null;
        String allWords;
        try {
            File file = new File(getNoAccentCachePath());
            if (file.exists()) {
                Log.i("frdict", dictName + " - allWordsNoAccent cache file found, using it");
                BufferedReader br = new BufferedReader(new FileReader(file));
                allWords = br.readLine();
                br.close();
                wordList = Arrays.asList(allWords.split(";"));
            }
        }
        catch (Exception e) {
            Log.e("frdict", dictName + " - Error getting no accent word list.", e);
        }
        return wordList;
    }

    /**
     * Get the list of tables in this database
     * @return The list of tables in this database
     */
    public List<String> getTableList() {
        List<String> results = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery("SELECT name, sql FROM sqlite_master WHERE type = 'table'",
                    new String[] {});

            if (cursor.moveToFirst()) {
                int nameColumnIndex = cursor.getColumnIndex("name");
                while (!cursor.isAfterLast()) {
                    String name = cursor.getString(nameColumnIndex);
                    results.add(name);
                    cursor.moveToNext();
                }
            }

            cursor.close();
        }
        catch (Exception e) {
            Log.e("frdict", dictName + " - Something went wrong when querying offline database.", e);
        }
        return results;
    }

    /**
     * Drop the full-text search table (or rather, tables) on this database
     */
    public void dropFtsTable() {
        db.execSQL("DROP TABLE IF EXISTS fts");
    }

    /**
     * Check if the current database already has the full-text search table(s)
     * @return true if the fts table(s) already exists, false otherwise
     */
    public boolean hasFtsTable() {
        List<String> tables = getTableList();
        for (String table : tables) {
            if (table.startsWith("fts")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create the full-text search table if it doesn't already exists
     */
    public void createFtsTable() {
        if (hasFtsTable()) {
            return;
        }

        Toast.makeText(context, "Creating full-text search table for " + databaseFileName, Toast.LENGTH_SHORT).show();
        long startTime = System.currentTimeMillis();
        db.execSQL("CREATE VIRTUAL TABLE fts USING fts3 (name, definition)");
        db.execSQL("INSERT INTO fts SELECT * FROM word");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        Log.i("frdict", dictName + " - Created full-text search table, time: " + duration + "ms");
        Toast.makeText(context, "Done creating full-text search table for " + databaseFileName + " (" + duration + "ms)", Toast.LENGTH_SHORT).show();
    }
}
