package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.AuthResponse;
import com.aislam.rag.dto.DeviceChallengeRequest;
import com.aislam.rag.dto.DeviceChallengeResponse;
import com.aislam.rag.dto.DeviceSessionRequest;
import com.aislam.rag.dto.DeviceSessionResponse;
import com.aislam.rag.dto.LoginRequest;
import com.aislam.rag.dto.RegisterRequest;
import com.aislam.rag.dto.SocialLoginRequest;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.security.JwtService;
import com.aislam.rag.service.AppAttestService;
import com.aislam.rag.service.AuthService;
import com.aislam.rag.service.DeviceChallengeService;
import com.aislam.rag.service.PlayIntegrityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        value = "/api/auth",
        produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE,
        consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
)
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final DeviceChallengeService deviceChallengeService;
    private final PlayIntegrityService playIntegrityService;
    private final AppAttestService appAttestService;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            DeviceChallengeService deviceChallengeService,
            PlayIntegrityService playIntegrityService,
            AppAttestService appAttestService
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.deviceChallengeService = deviceChallengeService;
        this.playIntegrityService = playIntegrityService;
        this.appAttestService = appAttestService;
    }

    /** Tek kullanımlık challenge — Integrity / App Attest. */
    @PostMapping("/device-challenge")
    public ResponseEntity<DeviceChallengeResponse> deviceChallenge(
            @Valid @RequestBody DeviceChallengeRequest request
    ) {
        UUID deviceId = parseDeviceId(request.deviceId());
        String challenge = deviceChallengeService.issue(deviceId);
        return ResponseEntity.ok(new DeviceChallengeResponse(
                challenge,
                deviceChallengeService.ttlSeconds()
        ));
    }

    /**
     * Anonim cihaz oturumu.
     * Android: Play Integrity. iOS: App Attest. Secret APK/IPA’da yok.
     */
    @PostMapping("/device-session")
    public ResponseEntity<DeviceSessionResponse> deviceSession(@Valid @RequestBody DeviceSessionRequest request) {
        UUID deviceId = parseDeviceId(request.deviceId());
        String platform = request.platform() == null ? "" : request.platform().trim().toLowerCase();
        boolean appAttestRegistered = false;

        if ("ios".equals(platform) && appAttestService.isEnabled()) {
            deviceChallengeService.consume(request.challenge(), deviceId);
            appAttestRegistered = appAttestService.verifyIos(deviceId, request);
        } else if ((platform.isBlank() || "android".equals(platform)) && playIntegrityService.isEnabled()) {
            deviceChallengeService.consume(request.challenge(), deviceId);
            playIntegrityService.verifyAndroid(request.integrityToken(), request.challenge());
        }

        String token = jwtService.generateDeviceToken(deviceId);
        return ResponseEntity.ok(new DeviceSessionResponse(
                token,
                "Bearer",
                jwtService.deviceExpirationSeconds(),
                appAttestRegistered
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request.idToken()));
    }

    @PostMapping("/apple")
    public ResponseEntity<AuthResponse> loginWithApple(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithApple(request.idToken()));
    }

    private static UUID parseDeviceId(String raw) {
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new RagException("Geçersiz deviceId.", "DEVICE_ID_INVALID");
        }
    }
}
