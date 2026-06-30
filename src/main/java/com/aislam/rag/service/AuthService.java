package com.aislam.rag.service;

import com.aislam.rag.dto.AuthResponse;
import com.aislam.rag.dto.LoginRequest;
import com.aislam.rag.dto.RegisterRequest;
import com.aislam.rag.dto.UserProfileResponse;
import com.aislam.rag.entity.UserEntity;
import com.aislam.rag.exception.AuthException;
import com.aislam.rag.repository.UserRepository;
import com.aislam.rag.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AuthException("Bu e-posta adresi zaten kayıtlı.", "EMAIL_EXISTS");
        }

        UserEntity user = new UserEntity(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim()
        );
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("E-posta veya şifre hatalı.", "INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("E-posta veya şifre hatalı.", "INVALID_CREDENTIALS");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.expirationSeconds(),
                UserProfileResponse.from(user)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
