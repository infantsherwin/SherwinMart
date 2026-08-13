package com.sherwin.sherwinmart.controller;

import com.sherwin.sherwinmart.dao.UserDAO;
import com.sherwin.sherwinmart.dao.impl.UserDAOImpl;
import com.sherwin.sherwinmart.dto.ApiResponse;
import com.sherwin.sherwinmart.dto.UserResponseDTO;
import com.sherwin.sherwinmart.exception.AuthException;
import com.sherwin.sherwinmart.exception.ConflictException;
import com.sherwin.sherwinmart.exception.ValidationException;
import com.sherwin.sherwinmart.listener.DataSourceListener;
import com.sherwin.sherwinmart.model.User;
import com.sherwin.sherwinmart.service.UserService;
import com.sherwin.sherwinmart.util.JsonUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles F1: register, login, logout, and "who am I".
 * Thin controller — no SQL, no business rules; everything delegates to UserService.
 */
@WebServlet({"/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/logout", "/api/v1/auth/me"})
public class AuthServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AuthServlet.class);
    private UserService userService;

    @Override
    public void init() {
        UserDAO userDAO = new UserDAOImpl(DataSourceListener.getDataSource());
        this.userService = new UserService(userDAO);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        resp.setContentType("application/json");

        if (path.endsWith("/register")) {
            handleRegister(req, resp);
        } else if (path.endsWith("/login")) {
            handleLogin(req, resp);
        } else if (path.endsWith("/logout")) {
            handleLogout(req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.error("UNAUTHENTICATED", "Not logged in")));
            return;
        }
        Map<String, Object> me = Map.of(
                "id", session.getAttribute("userId"),
                "email", session.getAttribute("email"),
                "role", session.getAttribute("role"));
        resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(me)));
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<?, ?> body = JsonUtil.gson().fromJson(req.getReader(), Map.class);
        try {
            User user = userService.register(
                    str(body, "name"), str(body, "email"), str(body, "password"), str(body, "role"));
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(UserResponseDTO.fromEntity(user))));
        } catch (ValidationException e) {
            writeError(resp, 400, "VALIDATION_ERROR", e.getMessage());
        } catch (ConflictException e) {
            writeError(resp, 409, "CONFLICT", e.getMessage());
        } catch (SQLException e) {
            LOG.error("Registration failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Registration failed");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<?, ?> body = JsonUtil.gson().fromJson(req.getReader(), Map.class);
        try {
            User user = userService.login(str(body, "email"), str(body, "password"));

            // Session fixation prevention — regenerate session ID on login (Section 2 rule 3).
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = req.getSession(true);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute("userId", user.getId());
            session.setAttribute("email", user.getEmail());
            session.setAttribute("role", user.getRole().name());

            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(UserResponseDTO.fromEntity(user))));
        } catch (ValidationException e) {
            writeError(resp, 400, "VALIDATION_ERROR", e.getMessage());
        } catch (AuthException e) {
            writeError(resp, e.getStatusCode(), "AUTH_ERROR", e.getMessage());
        } catch (SQLException e) {
            LOG.error("Login failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Login failed");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(Map.of("loggedOut", true))));
    }

    private String str(Map<?, ?> body, String key) {
        Object val = body == null ? null : body.get(key);
        return val == null ? null : val.toString();
    }

    private void writeError(HttpServletResponse resp, int status, String code, String message) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.error(code, message)));
    }
}
