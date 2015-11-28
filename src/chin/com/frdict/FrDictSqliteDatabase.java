package chin.com.frdict;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import android.content.Context;

/**
 * Helper class for database provisioning
 * @author Chin
 *
 */
public class FrDictSqliteDatabase extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "wiktionary_fren.db";
    public static final int DATABASE_VERSION = 20151114;

    public FrDictSqliteDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgrade();
    }
}