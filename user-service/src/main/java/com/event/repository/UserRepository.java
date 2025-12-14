package com.event.repository;


import com.event.entity.User;
import com.event.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsById(UUID id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    Page<User> findByActive(boolean active, Pageable pageable);

    Page<User> findByVerified(boolean verified, Pageable pageable);

    @Query(value = "SELECT * FROM users WHERE ST_DWithin(location::geography, " +
            "ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)",
            nativeQuery = true)
    Iterable<User> findUsersWithinRadius(@Param("latitude") Double latitude,
                                         @Param("longitude") Double longitude,
                                         @Param("radiusMeters") Double radiusMeters);

    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END
        FROM User u
        JOIN u.role r
        WHERE u.id = :userId
        AND u.active = true
        AND (r = 'ADMIN' OR r = 'ROLE_ADMIN')
    """)
    boolean isAdminAndActive(@Param("userId") UUID userId);

    // OPTION 2: Return the User object only if they match criteria
    // (Useful if you need the user data afterwards)
    @Query("""
        SELECT u FROM User u
        JOIN u.role r
        WHERE u.id = :userId
        AND u.active = true
        AND (r = 'ADMIN' OR r = 'ROLE_ADMIN')
    """)
    Optional<User> findActiveAdminById(@Param("userId") UUID userId);

    Page<User> findAllByRole(UserRole role, Pageable pageable);
}
