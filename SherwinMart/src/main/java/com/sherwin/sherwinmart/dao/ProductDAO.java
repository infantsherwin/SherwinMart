package com.sherwin.sherwinmart.dao;

import com.sherwin.sherwinmart.model.Product;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data-access abstraction for product listings (DAO pattern, Section 12). */
public interface ProductDAO {

    Product create(Product product) throws SQLException;

    Optional<Product> findById(long id) throws SQLException;

    List<Product> search(String keyword, String category) throws SQLException;

    List<Product> findBySeller(long sellerId) throws SQLException;

    boolean update(Product product) throws SQLException;

    boolean delete(long id, long sellerId) throws SQLException;

    boolean decrementStock(long productId, int quantity) throws SQLException;
}
