package com.shopcart.user.repository;

import com.shopcart.common.repository.BaseRepository;
import com.shopcart.user.entity.Role;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends BaseRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
