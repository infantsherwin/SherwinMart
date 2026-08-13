package com.sherwin.sherwinmart.filter;

import com.sherwin.sherwinmart.dto.ApiResponse;
import com.sherwin.sherwinmart.util.JsonUtil;
import java.io.IOException;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Enforces a session check on protected servlets (Section 2 rule 3, Section 9 checklist).
 * Every servlet under /api/v1/** except the auth and health endpoints requires a logged-in
 * session; role-sensitive endpoints do their own additional role check in the service layer.
 */
@WebFilter("/api/v1/*")
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/health",
            "/api/v1/products"
    );

    @Override
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean isPublicGet = "GET".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/v1/products");

        if (PUBLIC_PATHS.contains(path) || isPublicGet) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(JsonUtil.gson().toJson(
                    ApiResponse.error("UNAUTHENTICATED", "Login required")));
            return;
        }
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        // no-op
    }
}
