package com.sherwin.sherwinmart.service;

import com.sherwin.sherwinmart.dao.CartDAO;
import com.sherwin.sherwinmart.dao.ProductDAO;
import com.sherwin.sherwinmart.dao.impl.OrderDAOImpl;
import com.sherwin.sherwinmart.exception.ConflictException;
import com.sherwin.sherwinmart.exception.ValidationException;
import com.sherwin.sherwinmart.model.CartItem;
import com.sherwin.sherwinmart.model.Order;
import com.sherwin.sherwinmart.model.OrderItem;
import com.sherwin.sherwinmart.model.Product;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OrderService {

    private final OrderDAOImpl orderDAO;
    private final CartDAO cartDAO;
    private final ProductDAO productDAO;

    public OrderService(OrderDAOImpl orderDAO, CartDAO cartDAO, ProductDAO productDAO) {
        this.orderDAO = orderDAO;
        this.cartDAO = cartDAO;
        this.productDAO = productDAO;
    }

    public Order placeOrder(long buyerId, boolean mockPaymentConfirmed)
            throws ValidationException, ConflictException, SQLException {
        if (!mockPaymentConfirmed) {
            throw new ValidationException("payment", "Mock payment confirmation is required to place an order");
        }

        List<CartItem> cartItems = cartDAO.findByUser(buyerId);
        if (cartItems.isEmpty()) {
            throw new ValidationException("cart", "Cart is empty");
        }

        try (Connection conn = orderDAO.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal total = BigDecimal.ZERO;
                java.util.Map<Long, Product> productMap = new java.util.LinkedHashMap<>();
                for (CartItem ci : cartItems) {
                    Product product = productDAO.findById(ci.getProductId())
                            .orElseThrow(() -> new IllegalStateException("Product no longer exists"));
                    if (product.getStockQty() < ci.getQuantity()) {
                        throw new ConflictException("Insufficient stock for: " + product.getName());
                    }
                    total = total.add(product.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
                    productMap.put(ci.getProductId(), product);
                }

                Order order = new Order();
                order.setBuyerId(buyerId);
                order.setStatus(Order.Status.CONFIRMED);
                order.setTotalAmount(total);
                orderDAO.createOrder(conn, order);

                for (CartItem ci : cartItems) {
                    Product product = productMap.get(ci.getProductId());
                    OrderItem item = new OrderItem();
                    item.setOrderId(order.getId());
                    item.setProductId(product.getId());
                    item.setQuantity(ci.getQuantity());
                    item.setUnitPrice(product.getPrice());
                    orderDAO.addOrderItem(conn, item);

                    boolean decremented = productDAO.decrementStock(conn, product.getId(), ci.getQuantity());
                    if (!decremented) {
                        throw new ConflictException("Insufficient stock for: " + product.getName());
                    }
                }

                cartDAO.clear(conn, buyerId);
                conn.commit();
                return order;
            } catch (ConflictException | RuntimeException e) {
                conn.rollback();
                if (e instanceof ConflictException) {
                    throw (ConflictException) e;
                }
                throw (RuntimeException) e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Order> buyerHistory(long buyerId) throws SQLException {
        return orderDAO.findByBuyer(buyerId);
    }

    public List<Order> sellerIncomingOrders(long sellerId) throws SQLException {
        return orderDAO.findBySeller(sellerId);
    }

    public List<OrderItem> orderItems(long orderId) throws SQLException {
        return orderDAO.findItemsByOrder(orderId);
    }
}