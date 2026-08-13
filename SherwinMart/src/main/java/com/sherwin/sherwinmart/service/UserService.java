package com.sherwin.sherwinmart.service;

import com.sherwin.sherwinmart.dao.UserDAO;
import com.sherwin.sherwinmart.exception.AuthException;
import com.sherwin.sherwinmart.exception.ConflictException;
import com.sherwin.sherwinmart.exception.ValidationException;
import com.sherwin.sherwinmart.model.User;
import com.sherwin.sherwinmart.util.PasswordUtil;
import com.sherwin.sherwinmart.util.ValidationUtil;
import java.sql.SQLException;

/** Business rules for registration and login (F1). No JDBC here — the DAO owns SQL. */
public class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User register(String name, String email, String password, String roleRaw)
            throws ValidationException, ConflictException, SQLException {
        if (ValidationUtil.isBlank(name)) {
            throw new ValidationException("name", "Name is required");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ValidationException("email", "A valid email is required");
        }
        if (password == null || password.length() < 8) {
            throw new ValidationException("password", "Password must be at least 8 characters");
        }
        User.Role role;
        try {
            role = User.Role.valueOf((roleRaw == null ? "" : roleRaw).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("role", "Role must be BUYER or SELLER");
        }
        if (role == User.Role.ADMIN) {
            // F1: admin is seed-only, never self-registered.
            throw new ValidationException("role", "Role must be BUYER or SELLER");
        }

        if (userDAO.findByEmail(email).isPresent()) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(role);
        return userDAO.create(user);
    }

    public User login(String email, String password) throws ValidationException, AuthException, SQLException {
        if (ValidationUtil.isBlank(email) || ValidationUtil.isBlank(password)) {
            throw new ValidationException("credentials", "Email and password are required");
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new AuthException("Invalid email or password", 401));
        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            throw new AuthException("Invalid email or password", 401);
        }
        return user;
    }

    public java.util.List<User> findAll() throws SQLException {
        return userDAO.findAll();
    }
}
