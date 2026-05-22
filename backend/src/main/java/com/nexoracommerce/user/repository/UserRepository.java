package com.nexoracommerce.user.repository;
import com.nexoracommerce.common.repository.BaseRepository;

import com.nexoracommerce.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for User entity
 */
@Repository
@Transactional(readOnly = true)
public interface UserRepository extends BaseRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
}

