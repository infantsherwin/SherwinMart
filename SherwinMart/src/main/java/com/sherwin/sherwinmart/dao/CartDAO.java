package com.sherwin.sherwinmart.dao;

import com.sherwin.sherwinmart.model.CartItem;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data-access abstraction for a buyer's cart (DAO pattern, Section 12). */
public interface CartDAO {

    List<CartItem> findByUser(long userId) throws SQLException;

    Optional<CartItem> findByUserAndProduct(long userId, long productId) throws SQLException;

    CartItem upsert(CartItem item) throws SQLException;

    boolean updateQuantity(long userId, long productId, int quantity) throws SQLException;

    boolean remove(long userId, long productId) throws SQLException;

    void clear(long userId) throws SQLException;
}
