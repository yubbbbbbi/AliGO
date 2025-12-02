package com.example.aligo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aligo.db.Store;
import com.example.aligo.geofence.GeofenceForegroundService;
import com.example.aligo.repository.StoreRepository;
import com.example.aligo.utils.CsvLoader;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;

public class MainActivity extends AppCompatActivity {

    // ---------------- UI ----------------
    private ViewPager2 pager;
    private View rootLayout, indicatorOlive, indicatorDaiso;
    private TextView locationText;
    private ImageView locationIcon;
    private BottomSheetBehavior<MaterialCardView> bottomSheetBehavior;
    private RecyclerView memoRecycler;
    private MemoAdapter memoAdapter;
    private ArrayList<MemoItem> memoList = new ArrayList<>();
    private TextView emptyText;
    private MemoDao memoDao;
    private String currentStore = "olive"; // 0=olive, 1=daiso
    private TextView todoCountText;

    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    // ---------------- Location & Geofence ----------------
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int REQ_FINE = 2001;
    private static final int REQ_BACKGROUND = 2002;

    private StoreRepository storeRepository;

    // 현재 GPS
    private Location lastMoveLocation = null;

    // 브랜드별 현재 기준 매장
    private Store currentOliveStore = null;
    private Store currentDaisoStore = null;
    private void makeStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    // ===============================================
    // onCreate()
    // ===============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeStatusBarTransparent();

        // Android 13+ 알림 권한
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        3003
                );
            }
        }

        setContentView(R.layout.activity_main);
        Log.d("CHECK", "=========== MainActivity onCreate() 실행 ==========");

        CsvLoader.loadStoresIfEmpty(this);
        storeRepository = new StoreRepository(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 메모 DB
        MemoDatabase db = MemoDatabase.getInstance(this);
        memoDao = db.memoDao();
        memoList.addAll(memoDao.getMemosByType("olive"));

        setupUI();
        checkFineLocationPermission();

        // ENTER 이벤트 처리
        handleGeofenceIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleGeofenceIntent(intent);
    }

    // ===============================================
    // ENTER 이벤트 처리
    // ===============================================
    private void handleGeofenceIntent(Intent intent) {
        if (intent == null) return;

        String storeName = intent.getStringExtra("trigger_store");
        if (storeName == null) return;

        Log.d("UI_EVENT", "🟢 지오펜스 ENTER 인텐트 수신: " + storeName);

        boolean isOlive = storeName.startsWith("올리브영");
        boolean isDaiso = storeName.startsWith("다이소");

        if (lastMoveLocation == null) return;

        // 매장명 파싱
        String rawName = storeName.replace("올리브영 ", "").replace("다이소 ", "");
        Store s = storeRepository.getStoreByName(rawName);
        if (s == null) return;

        Location storeLoc = new Location("");
        storeLoc.setLatitude(s.lat);
        storeLoc.setLongitude(s.lon);

        float newDist = lastMoveLocation.distanceTo(storeLoc);


        // ===============================================
        // 브랜드별로 기준 매장을 비교 후 더 가까우면 갱신
        // ===============================================

        if (isOlive) {

            // 이전 기준 매장 거리 비교
            if (currentOliveStore != null) {
                float oldDist = distanceTo(currentOliveStore);
                if (newDist > oldDist) {
                    Log.d("UI_EVENT", "🟢 올리브영: 기존 매장이 더 가까움 → 무시");
                    return;
                }
            }

            // 더 가까우므로 갱신
            currentOliveStore = s;
            Log.d("UI_EVENT", "🟢 올리브영 기준 매장 갱신됨 → " + s.name);

            // 현재 탭이 올리브라면 즉시 UI 반영
            if (currentStore.equals("olive"))
                updateLocationUI("올리브영 " + s.name, true);
        }

        if (isDaiso) {

            if (currentDaisoStore != null) {
                float oldDist = distanceTo(currentDaisoStore);
                if (newDist > oldDist) {
                    Log.d("UI_EVENT", "🟢 다이소: 기존 매장이 더 가까움 → 무시");
                    return;
                }
            }

            currentDaisoStore = s;
            Log.d("UI_EVENT", "🟢 다이소 기준 매장 갱신됨 → " + s.name);

            // 현재 탭이 다이소라면 즉시 UI 반영
            if (currentStore.equals("daiso"))
                updateLocationUI("다이소 " + s.name, false);
        }
    }

    private float distanceTo(Store store) {
        Location loc = new Location("");
        loc.setLatitude(store.lat);
        loc.setLongitude(store.lon);
        return lastMoveLocation.distanceTo(loc);
    }

    private void updateLocationUI(String fullText, boolean olive) {

        // fullText = "올리브영 대학로점" 또는 "다이소 혜화점"
        locationIcon.setImageResource(olive ? R.drawable.location_olive : R.drawable.location_red);

        // ----------------------------
        // 매장명 부분만 Bold 처리
        // ----------------------------
        int spaceIndex = fullText.indexOf(" "); // 첫 공백 위치 찾기
        SpannableStringBuilder ssb = new SpannableStringBuilder(fullText);

        if (spaceIndex != -1) {
            ssb.setSpan(
                    new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    spaceIndex + 1,        // 매장명 시작
                    fullText.length(),     // 끝까지
                    0
            );
        }

        locationText.setText(ssb);
        locationText.animate().alpha(1).setDuration(150).start();
    }


    // ===============================================
    // UI 초기화
    // ===============================================
    private void setupUI() {

        pager = findViewById(R.id.storePager);
        rootLayout = findViewById(R.id.rootLayout);
        locationText = findViewById(R.id.locationText);
        locationIcon = findViewById(R.id.locationIcon);
        indicatorOlive = findViewById(R.id.indicatorOlive);
        indicatorDaiso = findViewById(R.id.indicatorDaiso);
        emptyText = findViewById(R.id.emptyText);
        todoCountText = findViewById(R.id.todoCount);

        locationText.setText("가까운 매장 없음");

        MaterialCardView memoSheet = findViewById(R.id.memoSheet);
        ViewCompat.setOnApplyWindowInsetsListener(memoSheet, (v, insets) -> insets);

        BlurView sheetBlur = findViewById(R.id.sheetBlur);
        ViewGroup rootView = (ViewGroup) getWindow().getDecorView();
        sheetBlur.setupWith(rootView, new RenderEffectBlur()).setBlurRadius(25f);

        pager.setAdapter(new StorePagerAdapter(this));
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int pos) {

                currentStore = (pos == 0) ? "olive" : "daiso";

                loadStoreMemo();
                animateIndicators(pos);

                // 탭이 바뀌면 저장된 기준 매장 UI 반영
                if (currentStore.equals("olive")) {
                    if (currentOliveStore != null)
                        updateLocationUI("올리브영 " + currentOliveStore.name, true);
                    else
                        locationText.setText("가까운 매장 없음");
                        locationIcon.setImageResource(R.drawable.location_olive);  // ⭐ 추가

                } else {
                    if (currentDaisoStore != null)
                        updateLocationUI("다이소 " + currentDaisoStore.name, false);
                    else
                        locationText.setText("가까운 매장 없음");
                        locationIcon.setImageResource(R.drawable.location_red);    // ⭐ 추가

                }
            }
        });

        bottomSheetBehavior = BottomSheetBehavior.from(memoSheet);
        bottomSheetBehavior.setFitToContents(true);

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int peekHeight = (int)(screenHeight * 0.40f);
        bottomSheetBehavior.setPeekHeight(peekHeight);

        memoRecycler = findViewById(R.id.memoRecycler);
        memoRecycler.setLayoutManager(new LinearLayoutManager(this));
        memoAdapter = new MemoAdapter(
                memoList,
                memoDao,
                () -> {
                    updateEmptyState();
                    updateTodoCount();
                }
        );
        memoRecycler.setAdapter(memoAdapter);
        updateEmptyState();

        findViewById(R.id.addButton).setOnClickListener(v -> {
            memoAdapter.addMemo(currentStore);
            memoRecycler.scrollToPosition(0);
            loadStoreMemo();
        });

        // 키보드 감지로 바텀시트 제어
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootLayout.getWindowVisibleDisplayFrame(r);

            int keypadHeight = screenHeight - r.bottom;
            boolean isKeyboardVisible = keypadHeight > dp(100);

            if (isKeyboardVisible) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                bottomSheetBehavior.setDraggable(false);
            } else {
                bottomSheetBehavior.setPeekHeight((int)(screenHeight * 0.68f));
                bottomSheetBehavior.setDraggable(true);
            }
        });
    }

    // ===============================================
    // 권한 요청
    // ===============================================
    private void checkFineLocationPermission() {

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_FINE
            );
        } else {
            checkBackgroundPermission();
        }
    }

    private void checkBackgroundPermission() {

        if (Build.VERSION.SDK_INT >= 29) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQ_BACKGROUND
                );
                return;
            }
        }

        startLocationUpdates();

        Intent i = new Intent(this, GeofenceForegroundService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }


    // ===============================================
    // 위치 업데이트
    // ===============================================
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2500
        ).setMinUpdateIntervalMillis(2000).build();

        if (locationCallback == null) {

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult result) {
                    handleLocation(result.getLastLocation());
                }
            };
        }

        fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                getMainLooper()
        );
    }

    // 미국 → 한국 초기 보정 플래그
    private boolean initialFixed = false;

    private void handleLocation(Location loc) {

        if (loc == null) return;

        double lat = loc.getLatitude();
        double lon = loc.getLongitude();

        Log.d("LOCATION", "현재 위치 = " + lat + ", " + lon);

        // ---------------------------------------------------------
        // 1) 최초 1회만 — 미국 좌표면 한국 좌표로 강제 보정
        // ---------------------------------------------------------
        if (!initialFixed) {

            boolean isKorea = (lat >= 30 && lat <= 45 && lon >= 120 && lon <= 135);

            if (!isKorea) {
                Log.d("LOCATION", "첫 GPS가 해외 → 한국(서울)로 1회 보정");

                // 서울 좌표
                Location fixed = new Location("");
                fixed.setLatitude(37.5665);
                fixed.setLongitude(126.9780);

                lastMoveLocation = fixed;
                initialFixed = true;

                Log.d("LOCATION", "보정된 초기 좌표 = 37.5665, 126.9780");
                return;
            }

            // (한국이라면 즉시 초기 설정)
            lastMoveLocation = loc;
            initialFixed = true;

            Log.d("LOCATION", "초기 GPS(한국) 설정 완료");
            return;
        }

        // ---------------------------------------------------------
        // 2) 정상 이동 거리 계산
        // ---------------------------------------------------------
        if (lastMoveLocation != null) {
            float moved = lastMoveLocation.distanceTo(loc);
            Log.d("DEBUG_MOVE", "-> 이번 이동 거리 = " + moved + "m");
        }

        lastMoveLocation = loc;
    }

    // ===============================================
    // 메모 관련
    // ===============================================
    private void loadStoreMemo() {
        memoList.clear();
        memoList.addAll(memoDao.getMemosByType(currentStore));
        memoAdapter.notifyDataSetChanged();
        updateEmptyState();
        updateTodoCount();
    }

    private void updateEmptyState() {
        emptyText.setVisibility(memoList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateTodoCount() {
        int count = memoList.size();
        String text = "할 일 " + count;

        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        int start = "할 일 ".length();
        int end = text.length();

        ssb.setSpan(
                new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start, end, 0
        );

        todoCountText.setText(ssb);
    }

    // ===============================================
    // Indicator animation
    // ===============================================
    private void animateIndicators(int position) {

        ValueAnimator oliveWidth = ValueAnimator.ofInt(
                indicatorOlive.getLayoutParams().width,
                position == 0 ? dp(20) : dp(8)
        );

        ValueAnimator daisoWidth = ValueAnimator.ofInt(
                indicatorDaiso.getLayoutParams().width,
                position == 1 ? dp(20) : dp(8)
        );

        oliveWidth.addUpdateListener(anim -> {
            indicatorOlive.getLayoutParams().width = (int) anim.getAnimatedValue();
            indicatorOlive.requestLayout();
            indicatorOlive.setBackgroundTintList(
                    ColorStateList.valueOf(
                            position == 0 ? Color.parseColor("#787E1E") : Color.parseColor("#D0D0D0")
                    )
            );
        });

        daisoWidth.addUpdateListener(anim -> {
            indicatorDaiso.getLayoutParams().width = (int) anim.getAnimatedValue();
            indicatorDaiso.requestLayout();
            indicatorDaiso.setBackgroundTintList(
                    ColorStateList.valueOf(
                            position == 1 ? Color.parseColor("#9B0000") : Color.parseColor("#D0D0D0")
                    )
            );
        });

        oliveWidth.setDuration(200);
        daisoWidth.setDuration(200);
        oliveWidth.start();
        daisoWidth.start();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
