package com.nexoracommerce.user.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.user.entity.Role;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends BaseRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
