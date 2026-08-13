package com.sherwin.sherwinmart.dao;

import com.sherwin.sherwinmart.model.User;
import java.sql.SQLException;
import java.util.Optional;

/** Data-access abstraction for users (DAO pattern, Section 12). */
public interface UserDAO {

    User create(User user) throws SQLException;

    Optional<User> findById(long id) throws SQLException;

    Optional<User> findByEmail(String email) throws SQLException;

    java.util.List<User> findAll() throws SQLException;
}
