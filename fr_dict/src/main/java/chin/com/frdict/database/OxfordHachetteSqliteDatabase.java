package chin.com.frdict.database;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;
import chin.com.frdict.R;

/**
 * Helper class for working with the Oxford Hachette database
 * @author Chin
 */
public class OxfordHachetteSqliteDatabase extends BaseDictionarySqliteDatabase {
    public static final int DATABASE_VERSION = 20151206;
    private static OxfordHachetteSqliteDatabase instance;

    private OxfordHachetteSqliteDatabase(Context context) {
        super(context, "Oxford Hachette FR-EN", "oxford_hachette_v3.db");
    }

    public static OxfordHachetteSqliteDatabase getInstance(Context context) {
        if (instance == null) {
            OxfordHachetteSqliteDatabase dbHelper = new OxfordHachetteSqliteDatabase(context);
            File file = new File(dbHelper.getDatabasePath());
            File file2 = new File(dbHelper.getDatabaseAlternatePath());
            if (file.exists() || file2.exists()) {
                String path = file.exists()? dbHelper.getDatabasePath() : dbHelper.getDatabaseAlternatePath();

                // NOTE: We need OPEN_READWRITE if we want to create FTS tables on the fly, but
                // opening with OPEN_READWRITE results in an error in new Android versions
                dbHelper.db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
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

    @Override
    public String getWordDefinition(String name) {
        String definition = super.getWordDefinition(name);
        if (definition == null) {
            String definition1 = super.getWordDefinition(name + " (1)");
            if (definition1 != null) {
                definition = definition1;
                String definition2 = super.getWordDefinition(name + " (2)");
                if (definition2 != null) {
                    definition += definition2;
                }
            }
        }
        if (definition == null) return null;

        definition = "<style>" + context.getString(R.string.oxford_hachette_css) + "</style>" + definition;
        return definition;
    }
}
