package com.event.repository;


import com.event.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query(value = "SELECT * FROM users WHERE ST_DWithin(location::geography, " +
            "ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)",
            nativeQuery = true)
    Iterable<User> findUsersWithinRadius(@Param("latitude") Double latitude,
                                         @Param("longitude") Double longitude,
                                         @Param("radiusMeters") Double radiusMeters);
}
