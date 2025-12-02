package com.example.aligo.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "stores")
public class Store {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "address")
    public String address;

    @ColumnInfo(name = "lat")
    public double lat;
    @ColumnInfo(name = "lon")
    public double lon;

    @ColumnInfo(name = "brand")
    public String brand;

    @Ignore
    public double distance;

    // Room이 쓸 기본 생성자
    public Store() {}

    // 우리가 직접 insert할 때 쓰는 생성자
    public Store(String name, String address, double lat, double lon, String brand) {
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lon = lon;
        this.brand = brand;
    }
}
