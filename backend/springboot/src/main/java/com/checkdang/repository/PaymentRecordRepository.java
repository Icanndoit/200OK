package com.checkdang.repository;

import com.checkdang.domain.PaymentRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRecordRepository {

    private final DynamoDbTable<PaymentRecord> paymentTable;

    public PaymentRecord save(PaymentRecord record) {
        paymentTable.putItem(record);
        return record;
    }

    public Optional<PaymentRecord> findByUserIdAndOrderId(String userId, String orderId) {
        Key key = Key.builder()
                .partitionValue(userId)
                .sortValue(orderId)
                .build();
        return Optional.ofNullable(paymentTable.getItem(key));
    }

    public Optional<PaymentRecord> findByUserIdAndOrderIdAndStatus(
            String userId, String orderId, PaymentRecord.PaymentStatus status) {
        return findByUserIdAndOrderId(userId, orderId)
                .filter(r -> status.name().equals(r.getStatus()));
    }

    public List<PaymentRecord> findByUserIdOrderByCreatedAtDesc(String userId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(userId).build());
        return paymentTable.query(condition).items().stream()
                .sorted(Comparator.comparing(PaymentRecord::getCreatedAt, Comparator.reverseOrder()))
                .toList();
    }
}
