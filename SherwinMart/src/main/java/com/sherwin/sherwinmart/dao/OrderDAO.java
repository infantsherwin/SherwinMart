package com.sherwin.sherwinmart.dao;

import com.sherwin.sherwinmart.model.Order;
import com.sherwin.sherwinmart.model.OrderItem;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data-access abstraction for orders and order line items (DAO pattern, Section 12). */
public interface OrderDAO {

    Order createOrder(Connection conn, Order order) throws SQLException;

    void addOrderItem(Connection conn, OrderItem item) throws SQLException;

    Optional<Order> findById(long id) throws SQLException;

    List<Order> findByBuyer(long buyerId) throws SQLException;

    List<Order> findBySeller(long sellerId) throws SQLException;

    List<OrderItem> findItemsByOrder(long orderId) throws SQLException;

    boolean updateStatus(long orderId, Order.Status status) throws SQLException;
}
