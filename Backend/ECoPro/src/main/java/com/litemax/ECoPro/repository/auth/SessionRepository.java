package com.litemax.ECoPro.repository.auth;

import com.litemax.ECoPro.entity.auth.Session;
import com.litemax.ECoPro.entity.auth.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {
	Optional<Session> findByRefreshTokenAndActiveTrue(String refreshToken);
    List<Session> findByUserIdAndActiveTrue(Long userId);
}