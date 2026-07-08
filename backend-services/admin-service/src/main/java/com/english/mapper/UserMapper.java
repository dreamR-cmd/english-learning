package com.english.mapper;

import com.english.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserMapper extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
