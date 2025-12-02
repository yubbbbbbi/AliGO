package com.example.aligo;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MemoDao {

    @Query("SELECT * FROM memo_table ORDER BY id DESC")
    List<MemoItem> getAllMemos();

    @Query("SELECT * FROM memo_table WHERE storeType = :type ORDER BY id DESC")
    List<MemoItem> getMemosByType(String type);

    @Insert
    long insertMemo(MemoItem memo);

    @Update
    void updateMemo(MemoItem memo);

    @Delete
    void deleteMemo(MemoItem memo);
}
