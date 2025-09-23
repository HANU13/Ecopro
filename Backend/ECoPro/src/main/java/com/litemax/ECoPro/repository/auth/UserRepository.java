package com.litemax.ECoPro.repository.auth;


import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.litemax.ECoPro.entity.auth.Role;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.auth.User.UserStatus;




@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByEmailVerificationToken(String emailVerificationToken);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    long countByStatus(UserStatus userstatus);
    long countByCreatedAtAfter(LocalDateTime createdAt);
    long countByLastLoginAtAfter(LocalDateTime lastLoginAt);
    
//    @Query("SELECT u FROM User u " +
//    	       "WHERE (LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
//    	       "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
//    	       "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
//    	       "AND u.status = :status")
//    	Page<User> searchUsers(@Param("search") String search,
//    	                       @Param("status") UserStatus status,
//    	                       Pageable pageable);
    Page<User> findByStatus(UserStatus status,Pageable pageable);
    Page<User> findByStatusAndEmailContainingIgnoreCaseOrStatusAndFirstNameContainingIgnoreCaseOrStatusAndLastNameContainingIgnoreCase(
            UserStatus status1, String email,
            UserStatus status2, String firstName,
            UserStatus status3, String lastName,
            Pageable pageable);
    Page<User> findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndStatus(
            String email,
            String firstName,
            String lastName,
            UserStatus status,
            Pageable pageable
    );
    Page<User> findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String email,
            String firstName,
            String lastName,
            Pageable pageable
    );
    Page<User> findByRolesName(String roleName,Pageable pageable);
    long countByCreatedAtBetween(LocalDateTime startdate,LocalDateTime endDate);
    
    long countByRolesContaining(Role role);
}