package com.aligo.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import com.aligo.dao.TodoDAO;
import com.aligo.model.Todo;

// 모든 할 일 관련 요청을 /api/todo로 매핑합니다.
@WebServlet("/api/todo")
public class TodoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TodoDAO todoDAO; 

    // 서블릿 초기화 시 DAO 객체를 한 번만 생성합니다.
    @Override
    public void init() throws ServletException {
        this.todoDAO = new TodoDAO();
    }
    
    // JSON 응답을 클라이언트에 보내는 헬퍼 메서드
    private void sendJsonResponse(HttpServletResponse response, String jsonString) throws IOException {
        // 안드로이드 앱이 JSON을 파싱할 수 있도록 Content-Type을 지정합니다.
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print(jsonString);
        out.flush();
    }

    // =======================================================
    // 3. 메모 추가 (POST 요청)
    // =======================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        
        // 1. 요청 파라미터에서 메모 내용(content)을 가져옵니다.
        String content = request.getParameter("content");
        
        if (content == null || content.trim().isEmpty()) {
            sendJsonResponse(response, "{\"status\":\"error\", \"message\":\"Content cannot be empty.\"}");
            return;
        }
        
        // 2. DAO를 통해 DB에 메모를 삽입합니다.
        Todo newTodo = new Todo(content);
        boolean success = todoDAO.insertTodo(newTodo);
        
        // 3. 결과 응답 (JSON)
        if (success) {
            sendJsonResponse(response, "{\"status\":\"success\", \"message\":\"Memo added successfully.\"}");
        } else {
            sendJsonResponse(response, "{\"status\":\"error\", \"message\":\"Failed to add memo.\"}");
        }
    }

    // =======================================================
    // 1. 할 일 검색 / 2. 할 일 개수 표시 (GET 요청)
    // =======================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        
        // 요청 파라미터를 확인하여 검색, 개수 표시 중 어떤 동작인지 구분합니다.
        String keyword = request.getParameter("keyword");
        String action = request.getParameter("action"); 
        
        if ("count".equalsIgnoreCase(action)) {
            // 2. 할 일 개수 표시 (Count Action)
            int count = todoDAO.getRemainingCount();
            sendJsonResponse(response, "{\"status\":\"success\", \"count\":" + count + "}");
            return;
        }

        // 1. 할 일 검색 및 목록 표시 (List/Search Action)
        List<Todo> todos = todoDAO.selectTodos(keyword);
        
        // Todo 리스트를 JSON 문자열로 변환 (Gson 없이 수동으로 간단하게 변환)
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"status\":\"success\", \"todos\":[");
        
        for (int i = 0; i < todos.size(); i++) {
            Todo todo = todos.get(i);
            jsonBuilder.append("{");
            jsonBuilder.append("\"id\":").append(todo.getId()).append(",");
            jsonBuilder.append("\"content\":\"").append(todo.getContent()).append("\",");
            jsonBuilder.append("\"isCompleted\":").append(todo.isCompleted());
            jsonBuilder.append("}");
            if (i < todos.size() - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("]}");
        
        sendJsonResponse(response, jsonBuilder.toString());
    }

    // =======================================================
    // 4. 메모 체크 후 삭제 (PUT/DELETE 요청)
    // 체크(완료 처리)는 PUT을, 최종 삭제는 DELETE를 사용합니다.
    // =======================================================
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // DELETE 요청은 파라미터를 쿼리 문자열로 처리합니다.
        String idStr = request.getParameter("id");

        if (idStr == null) {
            sendJsonResponse(response, "{\"status\":\"error\", \"message\":\"ID is required for deletion.\"}");
            return;
        }
        
        try {
            int id = Integer.parseInt(idStr);
            
            // 4. 메모 삭제
            boolean success = todoDAO.deleteTodo(id);
            if (success) {
                sendJsonResponse(response, "{\"status\":\"success\", \"message\":\"Memo deleted successfully.\"}");
            } else {
                sendJsonResponse(response, "{\"status\":\"error\", \"message\":\"Failed to delete memo (ID not found).\"}");
            }

        } catch (NumberFormatException e) {
            sendJsonResponse(response, "{\"status\":\"error\", \"message\":\"Invalid ID format.\"}");
        }
    }
    
    // PUT 요청은 주로 데이터 업데이트(체크/미체크)에 사용됩니다.
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idStr = request.getParameter("id");

        if (idStr == null) {
             sendJsonResponse(response, "{\"status\":\"error\", \"message\":\"ID is required for update.\"}");
            return;
        }
        
        try {
            int id = Integer.parseInt(idStr);
            // 4. 메모 체크 (완료 처리)
            boolean success = todoDAO.completeTodo(id);
            if (success) {
                sendJsonResponse(response, "{\"status\":\"success\", \"message\":\"Memo checked as complete (Completed=True).\"}");
            } else {
                sendJsonResponse(response, "{\"status\":\"error\", \"message\":\"Failed to check memo.\"}");
            }
        } catch (NumberFormatException e) {
            sendJsonResponse(response, "{\"status\":\"error\", \"message\":\"Invalid ID format.\"}");
        }
    }
}