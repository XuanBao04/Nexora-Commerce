package com.shopcart.user.repository;
import com.shopcart.common.repository.BaseRepository;

import com.shopcart.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends BaseRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
}

