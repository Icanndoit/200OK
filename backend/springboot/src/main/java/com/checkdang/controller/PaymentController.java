package com.checkdang.controller;

import com.checkdang.domain.PaymentRecord;
import com.checkdang.dto.ApiResponse;
import com.checkdang.dto.KakaoPayApproveRequest;
import com.checkdang.dto.KakaoPayApproveResponse;
import com.checkdang.dto.KakaoPayReadyRequest;
import com.checkdang.dto.KakaoPayReadyResponse;
import com.checkdang.service.KakaoPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final KakaoPayService kakaoPayService;

    // 결제 준비: 카카오 결제 페이지 URL 발급
    @PostMapping("/kakao/ready")
    public ResponseEntity<ApiResponse<KakaoPayReadyResponse>> kakaoReady(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody KakaoPayReadyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                kakaoPayService.ready(userDetails.getUsername(), request)));
    }

    // 결제 승인: 카카오 결제 완료 후 최종 확정
    @PostMapping("/kakao/approve")
    public ResponseEntity<ApiResponse<KakaoPayApproveResponse>> kakaoApprove(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody KakaoPayApproveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                kakaoPayService.approve(userDetails.getUsername(), request)));
    }

    // 결제 취소: 카카오 결제 페이지에서 취소 시 리다이렉트
    @GetMapping("/kakao/cancel")
    public ResponseEntity<ApiResponse<Void>> kakaoCancel() {
        return ResponseEntity.ok(ApiResponse.error("결제가 취소되었습니다."));
    }

    // 결제 실패: 카카오 결제 실패 시 리다이렉트
    @GetMapping("/kakao/fail")
    public ResponseEntity<ApiResponse<Void>> kakaoFail() {
        return ResponseEntity.ok(ApiResponse.error("결제에 실패했습니다."));
    }

    // 결제 이력 조회
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentRecord>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                kakaoPayService.getHistory(userDetails.getUsername())));
    }
}
