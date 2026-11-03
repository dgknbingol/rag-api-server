package com.aislam.rag.service;

import com.aislam.rag.auth.AppleTokenVerifier;
import com.aislam.rag.auth.GoogleTokenVerifier;
import com.aislam.rag.auth.SocialIdentity;
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

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppleTokenVerifier appleTokenVerifier;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifier googleTokenVerifier,
            AppleTokenVerifier appleTokenVerifier
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.appleTokenVerifier = appleTokenVerifier;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AuthException("Bu e-posta adresi zaten kayıtlı.", "EMAIL_EXISTS");
        }

        String displayName = request.displayName().trim();
        if (userRepository.existsByDisplayNameIgnoreCase(displayName)) {
            throw new AuthException("Bu kullanıcı adı zaten kullanılıyor.", "DISPLAY_NAME_EXISTS");
        }

        UserEntity user = new UserEntity(
                email,
                passwordEncoder.encode(request.password()),
                displayName
        );
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("E-posta veya şifre hatalı.", "INVALID_CREDENTIALS"));

        if (!user.isPasswordLoginEnabled()) {
            throw new AuthException(
                    "Bu hesap Google veya Apple ile oluşturuldu. Sosyal giriş kullanın.",
                    "SOCIAL_LOGIN_REQUIRED"
            );
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("E-posta veya şifre hatalı.", "INVALID_CREDENTIALS");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        return loginWithSocial(googleTokenVerifier.verify(idToken), true);
    }

    @Transactional
    public AuthResponse loginWithApple(String identityToken) {
        return loginWithSocial(appleTokenVerifier.verify(identityToken), false);
    }

    private AuthResponse loginWithSocial(SocialIdentity identity, boolean google) {
        UserEntity user = google
                ? userRepository.findByGoogleSub(identity.subject()).orElse(null)
                : userRepository.findByAppleSub(identity.subject()).orElse(null);

        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(normalizeEmail(identity.email())).orElse(null);
            if (user != null) {
                if (google) {
                    user.setGoogleSub(identity.subject());
                } else {
                    user.setAppleSub(identity.subject());
                }
            } else {
                user = new UserEntity(
                        normalizeEmail(identity.email()),
                        passwordEncoder.encode(UUID.randomUUID().toString()),
                        uniqueDisplayName(identity.displayName())
                );
                user.setPasswordLoginEnabled(false);
                if (google) {
                    user.setGoogleSub(identity.subject());
                } else {
                    user.setAppleSub(identity.subject());
                }
            }
            userRepository.save(user);
        }

        return buildAuthResponse(user);
    }

    private String uniqueDisplayName(String raw) {
        String base = raw == null ? "Kullanici" : raw.trim().replaceAll("\\s+", " ");
        if (base.length() > 70) {
            base = base.substring(0, 70);
        }
        if (base.isBlank()) {
            base = "Kullanici";
        }
        if (!userRepository.existsByDisplayNameIgnoreCase(base)) {
            return base;
        }
        for (int i = 0; i < 25; i++) {
            String candidate = base + " " + (100 + (int) (Math.random() * 900));
            if (!userRepository.existsByDisplayNameIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return base + " " + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
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
