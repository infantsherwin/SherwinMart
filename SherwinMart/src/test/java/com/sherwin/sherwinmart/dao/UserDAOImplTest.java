package com.sherwin.sherwinmart.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sherwin.sherwinmart.dao.impl.UserDAOImpl;
import com.sherwin.sherwinmart.model.User;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** DAO test against an embedded H2 instance, per Section 9's testing matrix. */
class UserDAOImplTest {

    private UserDAO userDAO;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(120), "
                    + "email VARCHAR(180) UNIQUE, password_hash VARCHAR(60), role VARCHAR(10), "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
        userDAO = new UserDAOImpl(ds);
    }

    @Test
    void createAndFindById_returnsSameUser() throws SQLException {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@sherwinmart.com");
        user.setPasswordHash("hashed");
        user.setRole(User.Role.BUYER);

        User created = userDAO.create(user);
        assertTrue(created.getId() > 0);

        var found = userDAO.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("test@sherwinmart.com", found.get().getEmail());
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() throws SQLException {
        assertFalse(userDAO.findByEmail("nobody@sherwinmart.com").isPresent());
    }
}
