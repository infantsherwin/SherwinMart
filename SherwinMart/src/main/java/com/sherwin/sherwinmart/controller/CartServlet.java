package com.sherwin.sherwinmart.controller;

import com.sherwin.sherwinmart.dao.CartDAO;
import com.sherwin.sherwinmart.dao.ProductDAO;
import com.sherwin.sherwinmart.dao.impl.CartDAOImpl;
import com.sherwin.sherwinmart.dao.impl.ProductDAOImpl;
import com.sherwin.sherwinmart.dto.ApiResponse;
import com.sherwin.sherwinmart.exception.NotFoundException;
import com.sherwin.sherwinmart.exception.ValidationException;
import com.sherwin.sherwinmart.listener.DataSourceListener;
import com.sherwin.sherwinmart.model.Product;
import com.sherwin.sherwinmart.service.CartService;
import com.sherwin.sherwinmart.util.JsonUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles F4: add, update, remove cart items; view cart with running total. */
@WebServlet("/api/v1/cart")
public class CartServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CartServlet.class);
    private CartService cartService;

    @Override
    public void init() {
        CartDAO cartDAO = new CartDAOImpl(DataSourceListener.getDataSource());
        ProductDAO productDAO = new ProductDAOImpl(DataSourceListener.getDataSource());
        this.cartService = new CartService(cartDAO, productDAO);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        Long userId = requireUserId(req, resp);
        if (userId == null) {
            return;
        }
        try {
            CartService.CartView view = cartService.viewCart(userId);
            List<Map<String, Object>> lines = new ArrayList<>();
            for (Map.Entry<Product, Integer> entry : view.getLines().entrySet()) {
                lines.add(Map.of(
                        "product", entry.getKey(),
                        "quantity", entry.getValue(),
                        "lineTotal", entry.getKey().getPrice().multiply(java.math.BigDecimal.valueOf(entry.getValue()))));
            }
            resp.getWriter().write(JsonUtil.gson().toJson(
                    ApiResponse.success(Map.of("lines", lines, "total", view.getTotal()))));
        } catch (SQLException e) {
            LOG.error("Cart view failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Could not load cart");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        Long userId = requireUserId(req, resp);
        if (userId == null) {
            return;
        }
        Map<?, ?> body = JsonUtil.gson().fromJson(req.getReader(), Map.class);
        try {
            long productId = ((Number) body.get("productId")).longValue();
            int quantity = body.get("quantity") == null ? 1 : ((Number) body.get("quantity")).intValue();
            cartService.addItem(userId, productId, quantity);
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(Map.of("added", true))));
        } catch (ValidationException e) {
            writeError(resp, 400, "VALIDATION_ERROR", e.getMessage());
        } catch (NotFoundException e) {
            writeError(resp, 404, "NOT_FOUND", e.getMessage());
        } catch (SQLException e) {
            LOG.error("Cart add failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Could not add to cart");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        Long userId = requireUserId(req, resp);
        if (userId == null) {
            return;
        }
        Map<?, ?> body = JsonUtil.gson().fromJson(req.getReader(), Map.class);
        try {
            long productId = ((Number) body.get("productId")).longValue();
            int quantity = ((Number) body.get("quantity")).intValue();
            cartService.updateQuantity(userId, productId, quantity);
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(Map.of("updated", true))));
        } catch (ValidationException e) {
            writeError(resp, 400, "VALIDATION_ERROR", e.getMessage());
        } catch (NotFoundException e) {
            writeError(resp, 404, "NOT_FOUND", e.getMessage());
        } catch (SQLException e) {
            LOG.error("Cart update failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Could not update cart");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        Long userId = requireUserId(req, resp);
        if (userId == null) {
            return;
        }
        String productIdParam = req.getParameter("productId");
        if (productIdParam == null) {
            writeError(resp, 400, "VALIDATION_ERROR", "productId is required");
            return;
        }
        try {
            cartService.removeItem(userId, Long.parseLong(productIdParam));
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(Map.of("removed", true))));
        } catch (NotFoundException e) {
            writeError(resp, 404, "NOT_FOUND", e.getMessage());
        } catch (SQLException e) {
            LOG.error("Cart remove failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Could not remove from cart");
        }
    }

    private Long requireUserId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            writeError(resp, 401, "UNAUTHENTICATED", "Login required");
            return null;
        }
        return (Long) session.getAttribute("userId");
    }

    private void writeError(HttpServletResponse resp, int status, String code, String message) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.error(code, message)));
    }
}
