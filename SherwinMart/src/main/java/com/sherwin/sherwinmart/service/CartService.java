package com.sherwin.sherwinmart.service;

import com.sherwin.sherwinmart.dao.CartDAO;
import com.sherwin.sherwinmart.dao.ProductDAO;
import com.sherwin.sherwinmart.exception.NotFoundException;
import com.sherwin.sherwinmart.exception.ValidationException;
import com.sherwin.sherwinmart.model.CartItem;
import com.sherwin.sherwinmart.model.Product;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Business rules for the cart (F4): add, update, remove, running total. */
public class CartService {

    private final CartDAO cartDAO;
    private final ProductDAO productDAO;

    public CartService(CartDAO cartDAO, ProductDAO productDAO) {
        this.cartDAO = cartDAO;
        this.productDAO = productDAO;
    }

    public CartItem addItem(long userId, long productId, int quantity)
            throws ValidationException, NotFoundException, SQLException {
        if (quantity <= 0) {
            throw new ValidationException("quantity", "Quantity must be at least 1");
        }
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (product.getStockQty() < quantity) {
            throw new ValidationException("quantity", "Not enough stock available");
        }
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return cartDAO.upsert(item);
    }

    public void updateQuantity(long userId, long productId, int quantity)
            throws ValidationException, NotFoundException, SQLException {
        if (quantity <= 0) {
            throw new ValidationException("quantity", "Quantity must be at least 1");
        }
        boolean updated = cartDAO.updateQuantity(userId, productId, quantity);
        if (!updated) {
            throw new NotFoundException("Cart item not found");
        }
    }

    public void removeItem(long userId, long productId) throws NotFoundException, SQLException {
        boolean removed = cartDAO.remove(userId, productId);
        if (!removed) {
            throw new NotFoundException("Cart item not found");
        }
    }

    /** Returns cart lines joined with product details, plus the running total (F4). */
    public CartView viewCart(long userId) throws SQLException {
        List<CartItem> items = cartDAO.findByUser(userId);
        Map<Product, Integer> lines = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            Product product = productDAO.findById(item.getProductId()).orElse(null);
            if (product == null) {
                continue;
            }
            lines.put(product, item.getQuantity());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return new CartView(lines, total);
    }

    /** Simple read view combining cart lines with their running total. */
    public static class CartView {
        private final Map<Product, Integer> lines;
        private final BigDecimal total;

        public CartView(Map<Product, Integer> lines, BigDecimal total) {
            this.lines = lines;
            this.total = total;
        }

        public Map<Product, Integer> getLines() {
            return lines;
        }

        public BigDecimal getTotal() {
            return total;
        }
    }
}
