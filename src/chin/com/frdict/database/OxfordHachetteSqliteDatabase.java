package chin.com.frdict.database;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.Toast;
import chin.com.frdict.R;

/**
 * Helper class for working with the Oxford Hachette database
 * @author Chin
 */
public class OxfordHachetteSqliteDatabase extends BaseDictionarySqliteDatabase {
    private static String DATABASE_NAME = Environment.getExternalStorageDirectory().getPath() + "/frdicts/oxford_hachette_v3.db";
    private static String DATABASE_NAME_2 = Environment.getExternalStorageDirectory().getPath() + "/Android/obb/frdicts/oxford_hachette_v3.db";
    public static int DATABASE_VERSION = 20151206;
    protected static BaseDictionarySqliteDatabase instance;

    private OxfordHachetteSqliteDatabase(Context context) {
        super(context, DATABASE_NAME, DATABASE_VERSION);
    }

    public static BaseDictionarySqliteDatabase getInstance(Context context) {
        if (instance == null) {
            OxfordHachetteSqliteDatabase dbHelper = new OxfordHachetteSqliteDatabase(context);
            File file = new File(DATABASE_NAME);
            File file2 = new File(DATABASE_NAME_2);
            if (file.exists()) {
                dbHelper.db = SQLiteDatabase.openDatabase(DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            }
            else if (file2.exists()) {
                dbHelper.db = SQLiteDatabase.openDatabase(DATABASE_NAME_2, null, SQLiteDatabase.OPEN_READONLY);
            }
            else {
                Toast.makeText(context, "oxford_hachette_v3.db not found", Toast.LENGTH_LONG).show();
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
