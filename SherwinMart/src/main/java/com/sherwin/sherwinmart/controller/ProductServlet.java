package com.sherwin.sherwinmart.controller;

import com.sherwin.sherwinmart.dao.ProductDAO;
import com.sherwin.sherwinmart.dao.impl.ProductDAOImpl;
import com.sherwin.sherwinmart.dto.ApiResponse;
import com.sherwin.sherwinmart.exception.AuthException;
import com.sherwin.sherwinmart.exception.NotFoundException;
import com.sherwin.sherwinmart.exception.ValidationException;
import com.sherwin.sherwinmart.listener.DataSourceListener;
import com.sherwin.sherwinmart.model.Product;
import com.sherwin.sherwinmart.model.User;
import com.sherwin.sherwinmart.service.ProductService;
import com.sherwin.sherwinmart.util.JsonUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
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
 * Handles F2 (seller CRUD) and F3 (buyer browse/search/filter).
 * GET /api/v1/products            -> search/browse (public, F3)
 * GET /api/v1/products?id=1       -> single product
 * GET /api/v1/products?mine=true  -> seller's own listings
 * POST/PUT/DELETE                 -> seller-only CRUD (F2)
 */
@WebServlet("/api/v1/products")
public class ProductServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ProductServlet.class);
    private ProductService productService;

    @Override
    public void init() {
        ProductDAO productDAO = new ProductDAOImpl(DataSourceListener.getDataSource());
        this.productService = new ProductService(productDAO);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        try {
            String idParam = req.getParameter("id");
            String mine = req.getParameter("mine");

            if (idParam != null) {
                Product product = productService.findById(Long.parseLong(idParam));
                resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(product)));
                return;
            }
            if ("true".equalsIgnoreCase(mine)) {
                Long sellerId = requireSellerId(req, resp);
                if (sellerId == null) {
                    return;
                }
                List<Product> mineList = productService.findBySeller(sellerId);
                resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(mineList)));
                return;
            }

            String keyword = req.getParameter("q");
            String category = req.getParameter("category");
            List<Product> results = productService.search(keyword, category);
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(results)));
        } catch (NotFoundException e) {
            writeError(resp, 404, "NOT_FOUND", e.getMessage());
        } catch (SQLException e) {
            LOG.error("Product query failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Could not load products");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        Long sellerId = requireSellerId(req, resp);
        if (sellerId == null) {
            return;
        }
        Product product = parseProduct(req);
        try {
            Product created = productService.create(sellerId, product);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(created)));
        } catch (ValidationException e) {
            writeError(resp, 400, "VALIDATION_ERROR", e.getMessage());
        } catch (SQLException e) {
            LOG.error("Product create failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Could not create product");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        Long sellerId = requireSellerId(req, resp);
        if (sellerId == null) {
            return;
        }
        Product product = parseProduct(req);
        try {
            Product updated = productService.update(sellerId, product);
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(updated)));
        } catch (ValidationException e) {
            writeError(resp, 400, "VALIDATION_ERROR", e.getMessage());
        } catch (NotFoundException e) {
            writeError(resp, 404, "NOT_FOUND", e.getMessage());
        } catch (AuthException e) {
            writeError(resp, e.getStatusCode(), "FORBIDDEN", e.getMessage());
        } catch (SQLException e) {
            LOG.error("Product update failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Could not update product");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        Long sellerId = requireSellerId(req, resp);
        if (sellerId == null) {
            return;
        }
        String idParam = req.getParameter("id");
        if (idParam == null) {
            writeError(resp, 400, "VALIDATION_ERROR", "id is required");
            return;
        }
        try {
            productService.delete(sellerId, Long.parseLong(idParam));
            resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.success(Map.of("deleted", true))));
        } catch (NotFoundException e) {
            writeError(resp, 404, "NOT_FOUND", e.getMessage());
        } catch (AuthException e) {
            writeError(resp, e.getStatusCode(), "FORBIDDEN", e.getMessage());
        } catch (SQLException e) {
            LOG.error("Product delete failed", e);
            writeError(resp, 500, "SERVER_ERROR", "Could not delete product");
        }
    }

    private Product parseProduct(HttpServletRequest req) throws IOException {
        Map<?, ?> body = JsonUtil.gson().fromJson(req.getReader(), Map.class);
        Product product = new Product();
        if (body.get("id") != null) {
            product.setId(((Number) body.get("id")).longValue());
        }
        product.setName((String) body.get("name"));
        product.setDescription((String) body.get("description"));
        Object price = body.get("price");
        product.setPrice(price == null ? null : new BigDecimal(price.toString()));
        Object stock = body.get("stockQty");
        product.setStockQty(stock == null ? 0 : ((Number) stock).intValue());
        product.setCategory((String) body.get("category"));
        product.setImageUrl((String) body.get("imageUrl"));
        return product;
    }

    /** Ensures the caller is logged in as a SELLER; writes 401/403 and returns null otherwise. */
    private Long requireSellerId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            writeError(resp, 401, "UNAUTHENTICATED", "Login required");
            return null;
        }
        String role = (String) session.getAttribute("role");
        if (!User.Role.SELLER.name().equals(role)) {
            writeError(resp, 403, "FORBIDDEN", "Seller role required");
            return null;
        }
        return (Long) session.getAttribute("userId");
    }

    private void writeError(HttpServletResponse resp, int status, String code, String message) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(JsonUtil.gson().toJson(ApiResponse.error(code, message)));
    }
}
