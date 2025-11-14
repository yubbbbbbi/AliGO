package com.aligo.model;

public class Todo {
    private int id;
    private String content;
    private boolean isCompleted;
    // private String createdAt; // 현재는 불필요하여 생략 가능

    // 기본 생성자
    public Todo() {}

    // 데이터 추가 시 사용할 생성자
    public Todo(String content) {
        this.content = content;
        this.isCompleted = false;
    }

    // DB에서 조회 시 사용할 생성자
    public Todo(int id, String content, boolean isCompleted) {
        this.id = id;
        this.content = content;
        this.isCompleted = isCompleted;
    }

    // Getter와 Setter (필수)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }
}