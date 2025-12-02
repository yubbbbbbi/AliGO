package com.example.aligo.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.aligo.db.Store;
import com.example.aligo.db.StoreDao;
import com.example.aligo.db.StoreDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CsvLoader {

    public static void loadStoresIfEmpty(@NonNull Context context) {
        StoreDao storeDao = StoreDatabase.getInstance(context).storeDao();

        // DB가 비어있다면 CSV 로드
        if (storeDao.getAll().isEmpty()) {
            // 여기서 항상 3개 인자로 호출
            loadCsv(context, "daiso_geocoded.csv");
            loadCsv(context, "oliveyoung_geocoded.csv");
        }
    }

    // filename: CSV 파일 이름
    // brand   : "daiso" 또는 "olive"
    private static void loadCsv(Context context, String filename) {

        AssetManager assetManager = context.getAssets();
        StoreDao storeDao = StoreDatabase.getInstance(context).storeDao();

        try {
            InputStream is = assetManager.open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            reader.readLine(); // header skip

            // 파일 이름으로 브랜드 자동 분류
            String brand;
            if (filename.contains("daiso")) brand = "daiso";
            else if (filename.contains("olive")) brand = "olive";
            else brand = "unknown";

            while ((line = reader.readLine()) != null) {

                // 콤마 4개 미만인 행은 오류 → 스킵
                String[] cols = line.split(",");
                if (cols.length < 4) {
                    Log.e("CSV", "❌ 잘못된 행 → 스킵: " + line);
                    continue;
                }

                String name = cols[0];
                String address = cols[1];

                try {
                    double lat = Double.parseDouble(cols[2].trim());
                    double lon = Double.parseDouble(cols[3].trim());

                    Store store = new Store(name, address, lat, lon, brand);
                    storeDao.insert(store);

                } catch (NumberFormatException e) {
                    Log.e("CSV", "❌ 숫자 파싱 실패 → 스킵: " + line);
                    continue;  // 이 행 무시
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
