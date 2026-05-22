package com.nexoracommerce.payment.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.payment.entity.PaymentTransaction;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface PaymentTransactionRepository extends BaseRepository<PaymentTransaction, Long> {
}
