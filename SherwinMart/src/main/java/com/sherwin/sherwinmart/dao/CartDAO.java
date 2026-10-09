package com.sherwin.sherwinmart.dao;

import com.sherwin.sherwinmart.model.CartItem;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CartDAO {

    List<CartItem> findByUser(long userId) throws SQLException;

    Optional<CartItem> findByUserAndProduct(long userId, long productId) throws SQLException;

    CartItem upsert(CartItem item) throws SQLException;

    boolean updateQuantity(long userId, long productId, int quantity) throws SQLException;

    boolean remove(long userId, long productId) throws SQLException;

    void clear(long userId) throws SQLException;

    void clear(java.sql.Connection conn, long userId) throws SQLException;
}