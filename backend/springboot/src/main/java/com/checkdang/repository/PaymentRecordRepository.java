package com.checkdang.repository;

import com.checkdang.domain.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    Optional<PaymentRecord> findByOrderId(String orderId);

    Optional<PaymentRecord> findByOrderIdAndStatus(String orderId, PaymentRecord.PaymentStatus status);

    List<PaymentRecord> findByUserIdOrderByCreatedAtDesc(String userId);
}
