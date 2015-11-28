package chin.com.frdict;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * A class for making queries to our sqlite database
 * @author Chin
 */
public class DatabaseQuerier {
    private static SQLiteDatabase db;
    Context context;

    public DatabaseQuerier(Context context) {
        this.context = context;
    }

    public SQLiteDatabase getDatabase() {
        if (db == null) {
            FrDictSqliteDatabase dbHelper = new FrDictSqliteDatabase(context);
            db = dbHelper.getReadableDatabase();
        }
        return db;
    }
}
