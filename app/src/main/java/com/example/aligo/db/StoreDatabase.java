package com.example.aligo.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Store.class}, version = 4, exportSchema = false)
public abstract class StoreDatabase extends RoomDatabase {

    private static volatile StoreDatabase INSTANCE;

    public abstract StoreDao storeDao();

    public static StoreDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (StoreDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    StoreDatabase.class,
                                    "store_db"
                            )
                            // 엔티티 구조 바뀌면 기존 DB 지우고 새로 생성
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
