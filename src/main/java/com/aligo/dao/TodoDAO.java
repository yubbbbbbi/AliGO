package com.aligo.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.aligo.model.Todo; // Todo 모델을 사용하기 위해 import

// Todo 테이블에 대한 모든 DB 접근(CRUD)을 담당합니다.
public class TodoDAO {
    // ⚠️ 환경에 맞게 JDBC URL, ID, Password를 수정하세요.
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/aligo_db?serverTimezone=UTC";
    private static final String JDBC_USER = "root";
    // *** 여기에 실제 MySQL root 비밀번호를 입력하세요! ***
    private static final String JDBC_PASSWORD = "mingyeong680@"; 

    // JDBC 드라이버 로드 (단 한 번만 필요)
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("MySQL JDBC Driver를 찾을 수 없습니다.", e);
        }
    }

    // DB 연결을 얻는 내부 헬퍼 메서드
    private Connection getConnection() throws SQLException {
        // [중요] 비밀번호를 실제 비밀번호로 수정한 후 저장하세요.
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    // =======================================================
    // 3. 메모 추가 (Add Memo)
    // =======================================================
    public boolean insertTodo(Todo todo) {
        String sql = "INSERT INTO todos (content, is_completed) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, todo.getContent());
            pstmt.setBoolean(2, todo.isCompleted());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // =======================================================
    // 1. 할 일 검색 및 목록 표시 (Search and List)
    // =======================================================
    public List<Todo> selectTodos(String keyword) {
        List<Todo> todos = new ArrayList<>();
        // 미완료된 항목 중 키워드를 포함하는 항목만 조회합니다. (키워드가 null이면 전체 조회)
        String sql = "SELECT id, content, is_completed FROM todos WHERE is_completed = FALSE";
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql += " AND content LIKE ?";
        }
        sql += " ORDER BY created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (keyword != null && !keyword.trim().isEmpty()) {
                pstmt.setString(1, "%" + keyword + "%"); 
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String content = rs.getString("content");
                    boolean isCompleted = rs.getBoolean("is_completed");
                    todos.add(new Todo(id, content, isCompleted));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return todos;
    }

    // =======================================================
    // 2. 할 일 개수 표시 (Display Count)
    // =======================================================
    public int getRemainingCount() {
        // 미완료된 할 일의 개수를 셉니다.
        String sql = "SELECT COUNT(id) AS count FROM todos WHERE is_completed = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =======================================================
    // 4. 할 일 체크 (Check - is_completed 상태 변경)
    // =======================================================
    public boolean completeTodo(int id) {
        String sql = "UPDATE todos SET is_completed = TRUE WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 4. 할 일 삭제 (Delete) - 이 기능을 앱에서 직접 호출하여 최종 삭제합니다.
    public boolean deleteTodo(int id) {
        String sql = "DELETE FROM todos WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}