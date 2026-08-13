package com.sherwin.sherwinmart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.sherwin.sherwinmart.dao.UserDAO;
import com.sherwin.sherwinmart.exception.ConflictException;
import com.sherwin.sherwinmart.exception.ValidationException;
import com.sherwin.sherwinmart.model.User;
import com.sherwin.sherwinmart.util.PasswordUtil;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Service-layer test — DAO mocked per Section 9's testing matrix. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @Test
    void register_duplicateEmail_throwsConflict() throws Exception {
        when(userDAO.findByEmail("dup@sherwinmart.com"))
                .thenReturn(Optional.of(new User()));
        UserService service = new UserService(userDAO);

        assertThrows(ConflictException.class, () ->
                service.register("Name", "dup@sherwinmart.com", "password123", "BUYER"));
    }

    @Test
    void register_weakPassword_throwsValidation() {
        UserService service = new UserService(userDAO);
        assertThrows(ValidationException.class, () ->
                service.register("Name", "new@sherwinmart.com", "short", "BUYER"));
    }

    @Test
    void register_adminRole_rejected() {
        UserService service = new UserService(userDAO);
        assertThrows(ValidationException.class, () ->
                service.register("Name", "new@sherwinmart.com", "password123", "ADMIN"));
    }

    @Test
    void passwordUtil_hashRoundTrips() {
        String hash = PasswordUtil.hash("mySecret123");
        assertEquals(true, PasswordUtil.matches("mySecret123", hash));
    }
}
