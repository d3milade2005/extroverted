package com.cityvibe.admin.service;

import com.cityvibe.admin.exception.ResourceNotFoundException;
import com.cityvibe.admin.exception.UnauthorizedException;
import com.cityvibe.admin.model.AdminRole;
import com.cityvibe.admin.model.AdminUser;
import com.cityvibe.admin.model.Permission;
import com.cityvibe.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;

    @Transactional(readOnly = true)
    public AdminUser getByUserId(UUID userId) {
        return adminUserRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with userId: " + userId));
    }

    @Transactional(readOnly = true)
    public AdminUser getByUserIdOrNull(UUID userId) {
        return adminUserRepository.findByUserId(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(UUID userId) {
        return adminUserRepository.existsByUserIdAndIsActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public void verifyPermission(UUID userId, Permission permission) {
        AdminUser admin = getByUserId(userId);

        if (!admin.getIsActive()) {
            throw new UnauthorizedException("Admin account is deactivated");
        }

        if (!admin.hasPermission(permission)) {
            throw new UnauthorizedException("Admin does not have permission: " + permission.getDisplayName());
        }
    }

    @Transactional
    public void recordLogin(UUID userId) {
        AdminUser admin = getByUserId(userId);
        admin.recordLogin();
        adminUserRepository.save(admin);
        log.info("Recorded login for admin {}", userId);
    }

    @Transactional
    public void recordAction(UUID userId) {
        AdminUser admin = getByUserId(userId);
        admin.recordAction();
        adminUserRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public List<AdminUser> getAllActiveAdmins() {
        return adminUserRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public long countActiveAdmins() {
        return adminUserRepository.countActiveAdmins();
    }
}