package com.sherwin.sherwinmart.service;

import com.sherwin.sherwinmart.dao.ProductDAO;
import com.sherwin.sherwinmart.exception.AuthException;
import com.sherwin.sherwinmart.exception.NotFoundException;
import com.sherwin.sherwinmart.exception.ValidationException;
import com.sherwin.sherwinmart.model.Product;
import com.sherwin.sherwinmart.util.ValidationUtil;
import java.sql.SQLException;
import java.util.List;

/** Business rules for product listings (F2, F3). No JDBC here — the DAO owns SQL. */
public class ProductService {

    private final ProductDAO productDAO;

    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public Product create(long sellerId, Product product) throws ValidationException, SQLException {
        validate(product);
        product.setSellerId(sellerId);
        return productDAO.create(product);
    }

    public Product update(long sellerId, Product product) throws ValidationException, NotFoundException,
            AuthException, SQLException {
        validate(product);
        Product existing = productDAO.findById(product.getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (existing.getSellerId() != sellerId) {
            throw new AuthException("You do not own this listing", 403);
        }
        product.setSellerId(sellerId);
        productDAO.update(product);
        return product;
    }

    public void delete(long sellerId, long productId) throws NotFoundException, AuthException, SQLException {
        Product existing = productDAO.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (existing.getSellerId() != sellerId) {
            throw new AuthException("You do not own this listing", 403);
        }
        productDAO.delete(productId, sellerId);
    }

    public List<Product> search(String keyword, String category) throws SQLException {
        return productDAO.search(keyword, category);
    }

    public List<Product> findBySeller(long sellerId) throws SQLException {
        return productDAO.findBySeller(sellerId);
    }

    public Product findById(long id) throws NotFoundException, SQLException {
        return productDAO.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private void validate(Product product) throws ValidationException {
        if (ValidationUtil.isBlank(product.getName())) {
            throw new ValidationException("name", "Product name is required");
        }
        if (!ValidationUtil.isPositive(product.getPrice())) {
            throw new ValidationException("price", "Price must be greater than zero");
        }
        if (product.getStockQty() < 0) {
            throw new ValidationException("stockQty", "Stock quantity cannot be negative");
        }
        if (ValidationUtil.isBlank(product.getCategory())) {
            throw new ValidationException("category", "Category is required");
        }
    }
}
