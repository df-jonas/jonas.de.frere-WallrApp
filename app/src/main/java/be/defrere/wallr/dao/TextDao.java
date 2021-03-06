package be.defrere.wallr.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import be.defrere.wallr.entity.Text;

@Dao
public interface TextDao {

    @Query("SELECT * FROM texts")
    List<Text> all();

    @Query("SELECT * FROM texts where id=:id")
    Text findById(int id);

    @Query("SELECT * FROM texts where event_id=:eventid")
    List<Text> findByEvent(int eventid);

    @Query("SELECT * FROM texts ORDER BY id DESC LIMIT 1;")
    List<Text> last();

    @Insert
    void insert(Text text);

    @Update
    void update(Text text);

    @Delete
    void delete(Text text);
}
