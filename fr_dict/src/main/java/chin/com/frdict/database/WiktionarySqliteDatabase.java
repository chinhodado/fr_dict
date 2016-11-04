package chin.com.frdict.database;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

/**
 * Helper class for working with the Wiktionary database
 * @author Chin
 */
public class WiktionarySqliteDatabase extends BaseDictionarySqliteDatabase {
    public static final int DATABASE_VERSION = 20151114;
    protected static BaseDictionarySqliteDatabase instance;

    private WiktionarySqliteDatabase(Context context) {
        super(context, "Wiktionary FR-EN", "wiktionary_fren.db");
    }

    public static BaseDictionarySqliteDatabase getInstance(Context context) {
        if (instance == null) {
            WiktionarySqliteDatabase dbHelper = new WiktionarySqliteDatabase(context);
            File file = new File(dbHelper.getDatabasePath());
            File file2 = new File(dbHelper.getDatabaseAlternatePath());
            if (file.exists()) {
                dbHelper.db = SQLiteDatabase.openDatabase(dbHelper.getDatabasePath(), null, SQLiteDatabase.OPEN_READONLY);
            }
            else if (file2.exists()) {
                dbHelper.db = SQLiteDatabase.openDatabase(dbHelper.getDatabaseAlternatePath(), null, SQLiteDatabase.OPEN_READONLY);
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
        if (definition != null) {
            // In the current word's definition, if all the links all point to the same word,
            // append that word's definition to the current definition. This is useful for word
            // forms (e.g. conjugated verbs, plural form of nouns, etc.)
            Pattern p = Pattern.compile("<i>(\\w+)</i>");
            Matcher m = p.matcher(definition);
            Set<String> secondWords = new HashSet<>();
            while (m.find()) {
                String secondWord = m.group(1);
                if (secondWord.equals("plural") || secondWord.equals("feminine")) {
                    continue;
                }
                secondWords.add(secondWord);
            }

            if (secondWords.size() == 1) {
                String secondDefinition = super.getWordDefinition((String)secondWords.toArray()[0]);
                definition += secondDefinition;
            }

            String css = "<style> a { color: rgba(54, 95, 145, 1); } </style>";
            // this is just a heuristic and won't be accurate in all cases
            definition = definition.replaceAll("<i>(\\w+)</i>", "<a href='frdict://search\\?word=$1'><i>$1</i></a>");
            definition = css + definition;
        }
        return definition;
    }
}