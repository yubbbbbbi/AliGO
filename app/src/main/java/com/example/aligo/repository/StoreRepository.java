package com.example.aligo.repository;

import android.content.Context;

import com.example.aligo.db.Store;
import com.example.aligo.db.StoreDao;
import com.example.aligo.db.StoreDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StoreRepository {

    private StoreDao storeDao;

    public StoreRepository(Context context) {
        storeDao = StoreDatabase.getInstance(context).storeDao();
    }

    // Decrease 단계: 도/시 텍스트 필터링
    // Decrease 단계: 도/시 + 브랜드 텍스트 필터링
    private List<Store> filterByProvinceAndBrand(List<Store> input, String province, String brand) {
        List<Store> out = new ArrayList<>();
        for (Store s : input) {
            if (s.address != null
                    && s.address.startsWith(province)
                    && s.brand != null
                    && s.brand.equalsIgnoreCase(brand)) {
                out.add(s);
            }
        }
        return out;
    }

    // 거리 계산
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3; // meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Top N 찾기
    // Top N 찾기 (브랜드별)
    public List<Store> findTopNNearestStoresByBrand(
            double userLat, double userLon, String province, String brand, int topN) {

        List<Store> all = storeDao.getAll();

        // 1) 도/시 + 브랜드 필터
        List<Store> filtered = filterByProvinceAndBrand(all, province, brand);

        // 2) 거리 계산
        for (Store s : filtered) {
            s.distance = haversine(userLat, userLon, s.lat, s.lon);
        }

        // 3) 거리 순 정렬
        filtered.sort(Comparator.comparingDouble(s -> s.distance));

        // 4) Top N 잘라서 반환
        return filtered.subList(0, Math.min(topN, filtered.size()));
    }

    // 위도만
    public List<Double> extractLatitudes(List<Store> stores) {
        List<Double> out = new ArrayList<>();
        for (Store s : stores) out.add(s.lat);
        return out;
    }

    // 경도만
    public List<Double> extractLongitudes(List<Store> stores) {
        List<Double> out = new ArrayList<>();
        for (Store s : stores) out.add(s.lon);
        return out;
    }

    // 지점명만
    public List<String> extractNames(List<Store> stores) {
        List<String> out = new ArrayList<>();
        for (Store s : stores) out.add(s.name);
        return out;
    }

    public Store getStoreByName(String name) {
        return storeDao.getStoreByName(name);
    }

}
