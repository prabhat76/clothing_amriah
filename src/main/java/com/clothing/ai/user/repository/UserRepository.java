package com.clothing.ai.user.repository;

import com.clothing.ai.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :from")
    long countUsersFrom(@org.springframework.data.repository.query.Param("from") java.time.Instant from);
}
