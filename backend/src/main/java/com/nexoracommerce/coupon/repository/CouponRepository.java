package com.nexoracommerce.coupon.repository;
import com.nexoracommerce.common.repository.BaseRepository;

import com.nexoracommerce.coupon.entity.Coupon;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface CouponRepository extends BaseRepository<Coupon, String> {
}
