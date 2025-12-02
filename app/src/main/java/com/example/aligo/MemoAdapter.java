package com.example.aligo;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.MemoViewHolder> {

    private ArrayList<MemoItem> memoList;
    private MemoDao memoDao;

    // 메모 변화 알림 콜백
    private Runnable onListChanged;

    // 현재 수정 중인 메모 position
    private int editingPos = -1;

    // 콜백을 포함한 생성자
    public MemoAdapter(ArrayList<MemoItem> list, MemoDao dao, Runnable onListChanged) {
        this.memoList = list;
        this.memoDao = dao;
        this.onListChanged = onListChanged;
    }

    @NonNull
    @Override
    public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.memo_item, parent, false);
        return new MemoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoViewHolder h, int pos) {
        MemoItem item = memoList.get(pos);

        // 수정 모드
        if (editingPos == pos) {
            h.textMemo.setVisibility(View.GONE);
            h.editMemo.setVisibility(View.VISIBLE);
            h.editMemo.setText(item.getText());
            h.editMemo.requestFocus();
            showKeyboard(h.editMemo);

            h.editMemo.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    String newText = h.editMemo.getText().toString().replace("\n", "");
                    item.setText(newText);
                    memoDao.updateMemo(item);

                    editingPos = -1;
                    notifyItemChanged(pos);
                    hideKeyboard(v);

                    if (onListChanged != null) onListChanged.run();

                    return true;
                }
                return false;
            });

        } else {
            h.textMemo.setVisibility(View.VISIBLE);
            h.editMemo.setVisibility(View.GONE);
            h.textMemo.setText(item.getText());
        }

        // 체크박스 - 삭제 기능
        h.checkDone.setOnCheckedChangeListener(null);
        h.checkDone.setChecked(item.isChecked());
        h.checkDone.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {

                int realPos = h.getAdapterPosition();

                if (realPos != RecyclerView.NO_POSITION) {

                    MemoItem removeItem = memoList.get(realPos);
                    memoDao.deleteMemo(removeItem);

                    memoList.remove(realPos);
                    notifyItemRemoved(realPos);

                    if (onListChanged != null) onListChanged.run();
                }
            }
        });


        // 클릭하면 수정 모드
        h.textMemo.setOnClickListener(v -> {
            editingPos = pos;
            notifyItemChanged(pos);
        });

        // Enter → 저장
        h.editMemo.setOnKeyListener((v, keyCode, event) -> {
            if ((keyCode == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN)) {

                // 줄바꿈 제거
                String newText = h.editMemo.getText().toString().replace("\n", "");

                item.setText(newText);
                memoDao.updateMemo(item);

                editingPos = -1;
                notifyItemChanged(pos);

                hideKeyboard(v);

                if (onListChanged != null) onListChanged.run();

                return true; // 엔터 기본 동작 막기 (줄바꿈 방지)
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return memoList.size();
    }

    // =====================
    // 자동 저장 + 수정모드 해제
    // =====================
    public void clearFocus(RecyclerView rv) {
        if (editingPos != -1) {
            MemoViewHolder h = (MemoViewHolder) rv.findViewHolderForAdapterPosition(editingPos);

            if (h != null) {
                String newText = h.editMemo.getText().toString();
                MemoItem item = memoList.get(editingPos);

                item.setText(newText);
                memoDao.updateMemo(item);
            }
        }

        editingPos = -1;
        notifyDataSetChanged();

        if (onListChanged != null) onListChanged.run();  // 리스트 변화 콜백
    }

    // =====================
    // storeType 포함 메모 추가
    // =====================
    public void addMemo(String storeType) {
        MemoItem item = new MemoItem("", storeType);

        long id = memoDao.insertMemo(item);
        item.setId((int) id);

        memoList.add(0, item);
        editingPos = 0;
        notifyItemInserted(0);

        if (onListChanged != null) onListChanged.run();  // 리스트 변화 콜백
    }

    // =====================
    // 키보드 처리
    // =====================
    private void hideKeyboard(View v) {
        InputMethodManager imm =
                (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void showKeyboard(View v) {
        v.post(() -> {
            InputMethodManager imm =
                    (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    // =====================
    // ViewHolder
    // =====================
    class MemoViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkDone;
        TextView textMemo;
        EditText editMemo;

        MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            checkDone = itemView.findViewById(R.id.checkDone);
            textMemo = itemView.findViewById(R.id.textMemo);
            editMemo = itemView.findViewById(R.id.editMemo);
        }
    }
}
