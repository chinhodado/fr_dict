package chin.com.frdict.database;

import android.content.Context;
import chin.com.frdict.R;

/**
 * Helper class for working with the Oxford Hachette database
 * @author Chin
 */
public class OxfordHachetteSqliteDatabase extends BaseDictionarySqliteDatabase {
    private static String DATABASE_NAME = "oxford hachette v3.db";
    public static int DATABASE_VERSION = 20151206;
    protected static BaseDictionarySqliteDatabase instance;

    private OxfordHachetteSqliteDatabase(Context context) {
        super(context, DATABASE_NAME, DATABASE_VERSION);
    }

    public static BaseDictionarySqliteDatabase getInstance(Context context) {
        if (instance == null) {
            OxfordHachetteSqliteDatabase dbHelper = new OxfordHachetteSqliteDatabase(context);
            dbHelper.db = dbHelper.getReadableDatabase();
            instance = dbHelper;
        }
        return instance;
    }

    @Override
    public String getWordDefinition(String name) {
        String definition = super.getWordDefinition(name);
        definition = "<style>" + context.getString(R.string.oxford_hachette_css) + "</style>" + definition;
        return definition;
    }
}
