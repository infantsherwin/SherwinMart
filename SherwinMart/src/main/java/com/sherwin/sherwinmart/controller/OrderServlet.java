package com.sherwin.sherwinmart.controller;

import com.sherwin.sherwinmart.dao.CartDAO;
import com.sherwin.sherwinmart.dao.ProductDAO;
import com.sherwin.sherwinmart.dao.impl.CartDAOImpl;
import com.sherwin.sherwinmart.dao.impl.OrderDAOImpl;
import com.sherwin.sherwinmart.dao.impl.ProductDAOImpl;
import com.sherwin.sherwinmart.dto.ApiResponse;
import com.sherwin.sherwinmart.exception.ConflictException;
import com.sherwin.sherwinmart.exception.ValidationException;
import com.sherwin.sherwinmart.listener.DataSourceListener;
import com.sherwin.sherwinmart.model.Order;
import com.sherwin.sherwinmart.model.User;
import com.sherwin.sherwinmart.service.OrderService;
import com.sherwin.sherwinmart.util.JsonUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles F5 (checkout via mock payment) and F6 (order history for buyer and seller).
 * POST /api/v1/orders            -> place order from cart (buyer)
 * GET  /api/v1/orders            -> buyer's own order history
 * GET  /api/v1/orders?asSeller=true -> seller's incoming orders
 * GET  /api/v1/orders?id=1&items=true -> line items for one order
 */
@WebServlet("/api/v1/orders")
public class OrderServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(OrderServlet.class);
    private OrderService orderService;

    @Override
    public void init() {
        OrderDAOImpl orderDAO = new OrderDAOImpl(DataSourceListener.getDataSource());
        CartDAO cartDAO = new CartDAOImpl(DataSourceListener.getDataSource());
        ProductDAO productDAO = new ProductDAOImpl(DataSourceListener.getDataSource());
        this.orderService = new OrderService(orderDAO, cartDAO, productDAO);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        Long buyerId = requireUserId(req, resp);
        if (buyerId == null) {
            return;
        }
        Map<?, ?> body = JsonUtil.gson().fromJson(req.getReader(), Map.class);
        boolean mockPaymentConfirmed = body != null && Boolean.TRUE.equals(body.get("mockPaymentConfirmed"));
        try {
            Order order = orderService.placeOrder(buyerId, mockPaymentConfirmed);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(order)));
        } catch (ValidationException e) {
            writeError(resp, 400, "VALIDATION_ERROR", e.getMessage());
        } catch (ConflictException e) {
            writeError(resp, 409, "CONFLICT", e.getMessage());
        } catch (SQLException e) {
            LOG.error("Order placement failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Could not place order");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            writeError(resp, 401, "UNAUTHENTICATED", "Login required");
            return;
        }
        long userId = (Long) session.getAttribute("userId");

        try {
            String idParam = req.getParameter("id");
            if (idParam != null && "true".equalsIgnoreCase(req.getParameter("items"))) {
                resp.getWriter().write(JsonUtil.gson().toJson(
                        ApiResponse.success(orderService.orderItems(Long.parseLong(idParam)))));
                return;
            }

            if ("true".equalsIgnoreCase(req.getParameter("asSeller"))) {
                String role = (String) session.getAttribute("role");
                if (!User.Role.SELLER.name().equals(role)) {
                    writeError(resp, 403, "FORBIDDEN", "Seller role required");
                    return;
                }
                List<Order> incoming = orderService.sellerIncomingOrders(userId);
                resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(incoming)));
                return;
            }

            List<Order> history = orderService.buyerHistory(userId);
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(history)));
        } catch (SQLException e) {
            LOG.error("Order history query failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Could not load orders");
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
