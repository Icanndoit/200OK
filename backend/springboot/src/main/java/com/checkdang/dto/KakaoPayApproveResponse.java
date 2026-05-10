package com.checkdang.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Builder
public class KakaoPayApproveResponse {

    private String orderId;
    private String itemName;
    private Integer amount;
    private Integer premiumMonths;
    private Instant premiumExpiresAt;  // 프리미엄 만료 일시
    private LocalDateTime approvedAt;  // 결제 승인 일시
}
