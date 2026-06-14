package com.example.mall.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("手机号已注册");
        }
        String salt = UUID.randomUUID().toString().replace("-", "");
        MallUser user = new MallUser();
        user.setUsername(request.username());
        user.setPhone(request.phone());
        user.setSalt(salt);
        user.setPasswordHash(hashPassword(request.password(), salt));
        user.setCreatedAt(LocalDateTime.now());
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse login(LoginRequest request) {
        MallUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        String passwordHash = hashPassword(request.password(), user.getSalt());
        if (!passwordHash.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return UserResponse.from(user);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("密码加密不可用", exception);
        }
    }
}
