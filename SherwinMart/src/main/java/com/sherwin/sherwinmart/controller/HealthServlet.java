package com.sherwin.sherwinmart.controller;

import com.sherwin.sherwinmart.listener.DataSourceListener;
import com.sherwin.sherwinmart.util.JsonUtil;
import java.io.IOException;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** GET /api/v1/health */
@WebServlet("/api/v1/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "UP");
        try (Connection conn = DataSourceListener.getDataSource().getConnection()) {
            body.put("db", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            body.put("db", "DOWN");
        }
        resp.setContentType("application/json");
        resp.getWriter().write(JsonUtil.gson().toJson(body));
    }
}