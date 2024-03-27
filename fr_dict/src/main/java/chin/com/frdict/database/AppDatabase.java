package chin.com.frdict.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.Room;
import androidx.room.TypeConverters;
import android.content.Context;

@Database(entities = {SearchItem.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract SearchItemDao recentSearchDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                                    AppDatabase.class,
                                             "frdict-database")
                                   .build();

                }
            }
        }

        return INSTANCE;
    }
}
