package com.nexoracommerce.user.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.user.entity.UserAddress;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface UserAddressRepository extends BaseRepository<UserAddress, Long> {
    List<UserAddress> findByUserId(UUID userId);
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(UUID userId);
}
