package chin.com.frdict.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SearchItemDao {
    @Query("SELECT * FROM search_item")
    List<SearchItem> getAll();

    @Query("SELECT * FROM search_item WHERE uid IN (:ids)")
    List<SearchItem> loadAllByIds(int[] ids);

    @Query("SELECT * FROM search_item WHERE text LIKE :text ORDER BY date DESC LIMIT 1")
    SearchItem findByName(String text);

    @Query("SELECT count(*) as count, text FROM search_item group by text")
    List<CountedSearchItem> getAllDistinct();

    @Insert
    void insertAll(SearchItem... items);

    @Delete
    void delete(SearchItem item);
}
