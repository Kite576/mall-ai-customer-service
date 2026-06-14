package com.example.mall.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank @Size(min = 3, max = 20) String username,
                              @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
                              @NotBlank @Size(min = 6, max = 32) String password) {
}
