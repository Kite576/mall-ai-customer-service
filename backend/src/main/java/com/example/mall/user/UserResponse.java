package com.example.mall.user;

import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String phone, LocalDateTime createdAt) {

    public static UserResponse from(MallUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getPhone(), user.getCreatedAt());
    }
}
