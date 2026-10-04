package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.RegisterPushRequest;
import com.aislam.rag.dto.TestPushRequest;
import com.aislam.rag.dto.UnregisterPushRequest;
import com.aislam.rag.dto.UpdatePushPrefsRequest;
import com.aislam.rag.service.DevicePushTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/notifications", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class NotificationPushController {

    private final DevicePushTokenService devicePushTokenService;

    public NotificationPushController(DevicePushTokenService devicePushTokenService) {
        this.devicePushTokenService = devicePushTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterPushRequest request) {
        return ResponseEntity.ok(devicePushTokenService.register(request));
    }

    @PutMapping("/prefs")
    public ResponseEntity<Map<String, Object>> updatePrefs(@Valid @RequestBody UpdatePushPrefsRequest request) {
        return ResponseEntity.ok(devicePushTokenService.updatePrefs(request));
    }

    @DeleteMapping("/register")
    public ResponseEntity<Map<String, Object>> unregister(@Valid @RequestBody UnregisterPushRequest request) {
        return ResponseEntity.ok(devicePushTokenService.unregister(request));
    }

    /** QA: anında test push. */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test(@Valid @RequestBody TestPushRequest request) {
        return ResponseEntity.ok(devicePushTokenService.sendTest(request));
    }
}
