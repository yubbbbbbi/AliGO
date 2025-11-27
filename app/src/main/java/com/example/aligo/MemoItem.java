package com.example.aligo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "memo_table")
public class MemoItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String text;
    private boolean isChecked;

    private String storeType;   // ⭐ 추가: 올리브/다이소 구분용

    public MemoItem() {}

    public MemoItem(String text, String storeType) {
        this.text = text;
        this.storeType = storeType;
        this.isChecked = false;
    }

    // ============ Getter & Setter ============
    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getText() { return text; }

    public void setText(String text) { this.text = text; }

    public boolean isChecked() { return isChecked; }

    public void setChecked(boolean checked) { isChecked = checked; }

    public String getStoreType() { return storeType; }

    public void setStoreType(String storeType) { this.storeType = storeType; }
}
