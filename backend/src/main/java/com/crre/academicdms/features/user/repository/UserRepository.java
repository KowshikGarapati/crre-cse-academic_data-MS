package com.crre.academicdms.features.user.repository;

import com.crre.academicdms.features.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByRegistrationNumber(String registrationNumber);

    Optional<User> findByUniqueId(String uniqueId);

    List<User> findByFullNameContainingIgnoreCase(String name);

    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.username) = LOWER(:identifier)
           OR LOWER(u.email) = LOWER(:identifier)
           OR (u.registrationNumber IS NOT NULL AND LOWER(u.registrationNumber) = LOWER(:identifier))
           OR (u.uniqueId IS NOT NULL AND LOWER(u.uniqueId) = LOWER(:identifier))
    """)
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByUniqueId(String uniqueId);
}
