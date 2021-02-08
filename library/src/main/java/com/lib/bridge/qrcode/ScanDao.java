package com.lib.bridge.qrcode;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ScanDao {

    @Insert
    void insert(Scan scan);

    @Update
    void update(Scan scan);

    @Delete
    void delete(Scan scan);

    @Query("DELETE FROM scans")
    void deleteAllScans();

    @Query("SELECT * FROM scans")
    LiveData<List<Scan>> getAllScans();
}