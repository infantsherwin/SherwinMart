package com.sherwin.sherwinmart.dao.impl;

import com.sherwin.sherwinmart.dao.OrderDAO;
import com.sherwin.sherwinmart.model.Order;
import com.sherwin.sherwinmart.model.OrderItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * JDBC implementation of {@link OrderDAO}. createOrder/addOrderItem accept a caller-owned
 * Connection so the service layer can compose them into a single transaction
 * (place-order must be atomic — see the D3 sequence diagram).
 */
public class OrderDAOImpl implements OrderDAO {

    private final DataSource dataSource;

    public OrderDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Order createOrder(Connection conn, Order order) throws SQLException {
        String sql = "INSERT INTO orders (buyer_id, status, total_amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, order.getBuyerId());
            ps.setString(2, order.getStatus().name());
            ps.setBigDecimal(3, order.getTotalAmount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    order.setId(keys.getLong(1));
                }
            }
        }
        return order;
    }

    @Override
    public void addOrderItem(Connection conn, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, item.getOrderId());
            ps.setLong(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitPrice());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Order> findById(long id) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Order> findByBuyer(long buyerId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE buyer_id = ? ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        }
        return orders;
    }

    @Override
    public List<Order> findBySeller(long sellerId) throws SQLException {
        String sql = "SELECT DISTINCT o.* FROM orders o "
                + "JOIN order_items oi ON oi.order_id = o.id "
                + "JOIN products p ON p.id = oi.product_id "
                + "WHERE p.seller_id = ? ORDER BY o.created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        }
        return orders;
    }

    @Override
    public List<OrderItem> findItemsByOrder(long orderId) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        List<OrderItem> items = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getLong("id"));
                    item.setOrderId(rs.getLong("order_id"));
                    item.setProductId(rs.getLong("product_id"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    item.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
                    items.add(item);
                }
            }
        }
        return items;
    }

    @Override
    public boolean updateStatus(long orderId, Order.Status status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, orderId);
            return ps.executeUpdate() > 0;
        }
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setBuyerId(rs.getLong("buyer_id"));
        order.setStatus(Order.Status.valueOf(rs.getString("status")));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        Timestamp ts = rs.getTimestamp("created_at");
        order.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return order;
    }
}
