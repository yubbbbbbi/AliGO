package com.example.aligo;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private View rootLayout, indicatorOlive, indicatorDaiso;
    private TextView locationText;
    private ImageView locationIcon;
    private BottomSheetBehavior<MaterialCardView> bottomSheetBehavior;

    private RecyclerView memoRecycler;
    private MemoAdapter memoAdapter;
    private ArrayList<MemoItem> memoList = new ArrayList<>();

    private TextView emptyText;  // ⭐ 메모 없을 때 문구
    private MemoDao memoDao;

    private String currentStore = "olive"; // ⭐ 기본값

    private final int oliveColor = Color.parseColor("#FBF9F2");
    private final int daisoColor = Color.parseColor("#F9F2F0");
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ⭐ Room DB
        MemoDatabase db = MemoDatabase.getInstance(this);
        memoDao = db.memoDao();

        // ⭐ 첫 화면 로딩 시 올리브 메모 불러오기
        memoList.addAll(memoDao.getMemosByType("olive"));

        // ============================
        //  UI 연결
        // ============================
        pager = findViewById(R.id.storePager);
        rootLayout = findViewById(R.id.rootLayout);
        locationText = findViewById(R.id.locationText);
        locationIcon = findViewById(R.id.locationIcon);
        indicatorOlive = findViewById(R.id.indicatorOlive);
        indicatorDaiso = findViewById(R.id.indicatorDaiso);

        emptyText = findViewById(R.id.emptyText);

        MaterialCardView memoSheet = findViewById(R.id.memoSheet);
        ViewCompat.setOnApplyWindowInsetsListener(memoSheet, (v, insets) -> insets);

        // ============================
        //  BlurView 설정
        // ============================
        BlurView sheetBlur = findViewById(R.id.sheetBlur);
        setupSheetBlur(sheetBlur);

        // ============================
        //  ViewPager 설정
        // ============================
        StorePagerAdapter adapter = new StorePagerAdapter(this);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(2);

        pager.setPageTransformer((page, position) -> {
            page.setTranslationX(-position * page.getWidth() * 0.25f);
            page.setAlpha(0.9f + (1 - Math.abs(position)) * 0.1f);
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {

                currentStore = (position == 0) ? "olive" : "daiso";

                loadMemosForCurrentStore();
                animateIndicators(position);
                animateLocationText(position);
            }
        });

        // ============================
        //  BottomSheet 설정
        // ============================
        bottomSheetBehavior = BottomSheetBehavior.from(memoSheet);
        bottomSheetBehavior.setFitToContents(true);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setDraggable(true);

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int peekHeightPx = (int) (screenHeight * 0.4f);
        bottomSheetBehavior.setPeekHeight(peekHeightPx);

        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootLayout.getWindowVisibleDisplayFrame(r);

            int totalHeight = rootLayout.getRootView().getHeight();
            int keypadHeight = totalHeight - r.bottom;
            boolean keyboardVisible = keypadHeight > dp(200);

            bottomSheetBehavior.setDraggable(!keyboardVisible);
        });

        // ============================
        //  RecyclerView
        // ============================
        memoRecycler = findViewById(R.id.memoRecycler);
        memoRecycler.setLayoutManager(new LinearLayoutManager(this));

        memoAdapter = new MemoAdapter(memoList, memoDao, this::updateEmptyState);
        memoRecycler.setAdapter(memoAdapter);
        memoRecycler.setNestedScrollingEnabled(false);

        updateEmptyState(); // ⭐ 최초 상태도 체크

        // ============================
        //  + 버튼
        // ============================
        ImageButton addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            memoAdapter.addMemo(currentStore);
            memoRecycler.scrollToPosition(0);
        });
    }

    // ==========================================================
    //   ⭐ 스토어 변경 시 해당 메모 로드
    // ==========================================================
    private void loadMemosForCurrentStore() {
        List<MemoItem> stored = memoDao.getMemosByType(currentStore);

        memoList.clear();
        memoList.addAll(stored);

        memoAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    // ==========================================================
    //  ⭐ 메모 없으면 안내문 보이기
    // ==========================================================
    private void updateEmptyState() {
        if (memoList.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

    // ==========================================================
    //  화면 터치 → EditText 포커스 해제 & 키보드 숨김
    // ==========================================================
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();

        View current = getCurrentFocus();

        if (current instanceof EditText) {

            int[] screenCoords = new int[2];
            current.getLocationOnScreen(screenCoords);

            Rect editRect = new Rect(
                    screenCoords[0],
                    screenCoords[1],
                    screenCoords[0] + current.getWidth(),
                    screenCoords[1] + current.getHeight()
            );

            if (!editRect.contains(x, y)) {
                memoAdapter.clearFocus(memoRecycler);
                hideKeyboard();
                current.clearFocus();
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    // ==========================================================
    //  BlurView 셋업
    // ==========================================================
    private void setupSheetBlur(BlurView blurView) {
        ViewGroup rootView = (ViewGroup) getWindow().getDecorView();
        float radius = 25f;
        blurView.setupWith(rootView, new RenderEffectBlur()).setBlurRadius(radius);
    }

    // ==========================================================
    //  인디케이터 애니메이션
    // ==========================================================
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

    // ==========================================================
    //  위치 텍스트 애니메이션
    // ==========================================================
    private void animateLocationText(int position) {
        locationText.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            if (position == 0) {
                locationText.setText("올리브영 대학로점 근처");
                locationIcon.setImageResource(R.drawable.location_olive);
            } else {
                locationText.setText("다이소 혜화점 근처");
                locationIcon.setImageResource(R.drawable.location_red);
            }
            locationText.animate().alpha(1f).setDuration(150).start();
        }).start();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
