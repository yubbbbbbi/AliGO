package com.example.aligo.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Store store);

    @Query("SELECT * FROM stores")
    List<Store> getAll();

    @Query("SELECT * FROM stores WHERE name = :name LIMIT 1")
    Store getStoreByName(String name);

}
