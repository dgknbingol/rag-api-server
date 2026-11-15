package com.aislam.rag.controller;

import com.aislam.rag.config.AdsConfigProperties;
import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.AdsConfigDto;
import com.aislam.rag.security.ApiSecretAuth;
import com.aislam.rag.security.DeviceSessionAuth;
import com.aislam.rag.security.JwtService;
import com.aislam.rag.service.AdsConfigService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class AdsConfigController {

    public static final String ADMIN_KEY_HEADER = "X-Config-Admin-Key";

    private final AdsConfigService adsConfigService;
    private final AdsConfigProperties adsConfigProperties;
    private final JwtService jwtService;

    public AdsConfigController(
            AdsConfigService adsConfigService,
            AdsConfigProperties adsConfigProperties,
            JwtService jwtService
    ) {
        this.adsConfigService = adsConfigService;
        this.adsConfigProperties = adsConfigProperties;
        this.jwtService = jwtService;
    }

    /** Mobil okuma — cihaz JWT (Bearer) gerekir. APK’da sabit secret yok. */
    @GetMapping(value = "/ads", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AdsConfigDto> getAdsConfig(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        DeviceSessionAuth.requireDeviceId(jwtService, authorization);
        return ResponseEntity.ok(adsConfigService.getAdsConfig());
    }

    /**
     * Sadece sunucu admin secret (Postman). Mobilde yok.
     * Header: X-Config-Admin-Key
     */
    @PutMapping(
            value = "/ads",
            consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"},
            produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE
    )
    public ResponseEntity<AdsConfigDto> putAdsConfig(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey,
            @RequestBody AdsConfigDto body
    ) {
        requireAdmin(adminKey);
        return ResponseEntity.ok(adsConfigService.updateAdsConfig(body));
    }

    private void requireAdmin(String adminKey) {
        ApiSecretAuth.requireConfigured(
                adsConfigProperties.isAdminConfigured(),
                "ADS_CONFIG_ADMIN_DISABLED",
                "Ads config admin key tanımlı değil (ADS_CONFIG_ADMIN_KEY)."
        );
        ApiSecretAuth.requireMatch(
                adminKey,
                adsConfigProperties.adminKey(),
                "ADS_CONFIG_FORBIDDEN",
                "Geçersiz admin anahtarı."
        );
    }
}
