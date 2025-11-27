package com.example.aligo;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {MemoItem.class}, version = 3, exportSchema = false)
public abstract class MemoDatabase extends RoomDatabase {

    private static volatile MemoDatabase INSTANCE;

    public abstract MemoDao memoDao();

    public static MemoDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MemoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    MemoDatabase.class,
                                    "memo_database"
                            )
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
