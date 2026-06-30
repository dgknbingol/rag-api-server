package com.aislam.rag.service;

import com.aislam.rag.config.AuthProperties;
import com.aislam.rag.dto.LoginRequest;
import com.aislam.rag.dto.RegisterRequest;
import com.aislam.rag.exception.AuthException;
import com.aislam.rag.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({AuthService.class, JwtService.class, AuthServiceTest.TestConfig.class})
@EnableConfigurationProperties(AuthProperties.class)
@TestPropertySource(properties = {
        "app.auth.jwt-secret=dev-only-change-me-use-at-least-32-characters-long",
        "app.auth.jwt-expiration-hours=168"
})
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void registerAndLoginReturnsJwtToken() {
        var registerRequest = new RegisterRequest("test@example.com", "password123", "Test User");
        var authResponse = authService.register(registerRequest);

        assertNotNull(authResponse.token());
        assertEquals("Bearer", authResponse.tokenType());
        assertEquals("test@example.com", authResponse.user().email());
        assertTrue(authResponse.expiresInSeconds() > 0);

        var loginResponse = authService.login(new LoginRequest("test@example.com", "password123"));
        assertNotNull(loginResponse.token());
        assertEquals(authResponse.user().id(), loginResponse.user().id());
    }

    @Test
    void duplicateEmailRegistrationFails() {
        authService.register(new RegisterRequest("dup@example.com", "password123", "Dup User"));

        AuthException ex = assertThrows(
                AuthException.class,
                () -> authService.register(new RegisterRequest("dup@example.com", "password123", "Dup User 2"))
        );
        assertEquals("EMAIL_EXISTS", ex.getCode());
    }

    @Test
    void loginWithWrongPasswordFails() {
        authService.register(new RegisterRequest("login@example.com", "password123", "Login User"));

        AuthException ex = assertThrows(
                AuthException.class,
                () -> authService.login(new LoginRequest("login@example.com", "wrong-password"))
        );
        assertEquals("INVALID_CREDENTIALS", ex.getCode());
    }

    static class TestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
