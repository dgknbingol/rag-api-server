package com.aislam.rag.service;

import com.aislam.rag.dto.UserProfileResponse;
import com.aislam.rag.entity.UserEntity;
import com.aislam.rag.exception.AuthException;
import com.aislam.rag.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UserEntity user) {
        return UserProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Kullanıcı bulunamadı.", "USER_NOT_FOUND"));
        return UserProfileResponse.from(user);
    }
}
