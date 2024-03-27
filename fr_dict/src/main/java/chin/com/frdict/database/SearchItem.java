package chin.com.frdict.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "search_item")
public class SearchItem {
    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "text")
    private String text;

    @ColumnInfo(name = "date")
    private Date date;

    public SearchItem(String text) {
        this.text = text;
        this.date = new Date();
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
