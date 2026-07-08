package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.UserProfileResponse;
import com.aislam.rag.dto.UpdateDisplayNameRequest;
import com.aislam.rag.entity.UserEntity;
import com.aislam.rag.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping(value = "/api/users", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(userService.getProfile(user));
    }

    @PatchMapping("/me/display-name")
    public ResponseEntity<UserProfileResponse> updateDisplayName(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UpdateDisplayNameRequest request
    ) {
        return ResponseEntity.ok(userService.updateDisplayName(user, request.displayName()));
    }
}
