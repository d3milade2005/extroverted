package com.event.repository;

import com.event.entity.AdminRole;
import com.event.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    // Find by user ID (from Keycloak)
    Optional<AdminUser> findByUserId(UUID userId);

    // Find active admins
    List<AdminUser> findByIsActiveTrue();

    // Find by role
    List<AdminUser> findByAdminRole(AdminRole role);

    // Check if user is admin
    boolean existsByUserIdAndIsActiveTrue(UUID userId);

    // Count active admins
    @Query("SELECT COUNT(a) FROM AdminUser a WHERE a.isActive = true")
    long countActiveAdmins();
}
