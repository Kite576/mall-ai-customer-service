package com.example.mall.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<MallUser, Long> {

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    Optional<MallUser> findByUsername(String username);
}
