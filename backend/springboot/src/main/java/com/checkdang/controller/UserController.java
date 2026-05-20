package com.checkdang.controller;

import com.checkdang.dto.ApiResponse;
import com.checkdang.dto.GuestConvertRequest;
import com.checkdang.dto.LoginRequest;
import com.checkdang.dto.LogoutRequest;
import com.checkdang.dto.SignupRequest;
import com.checkdang.dto.SocialLoginRequest;
import com.checkdang.dto.TokenResponse;
import com.checkdang.dto.UserResponse;
import com.checkdang.service.SocialLoginService;
import com.checkdang.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SocialLoginService socialLoginService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.login(request)));
    }

    @PostMapping("/social")
    public ResponseEntity<ApiResponse<TokenResponse>> socialLogin(@RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(socialLoginService.socialLogin(request)));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.ok(userService.checkEmailAvailable(email)));
    }

    @PostMapping("/guest-convert")
    public ResponseEntity<ApiResponse<TokenResponse>> guestConvert(@RequestBody GuestConvertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userService.guestConvert(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request) {
        userService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/guest")
    public ResponseEntity<ApiResponse<TokenResponse>> guestLogin() {
        return ResponseEntity.ok(ApiResponse.ok(userService.generateGuestToken()));
    }

    // Cognito 소셜 로그인 — 프론트가 Cognito에서 받은 ID Token을 Authorization 헤더로 전송
    // Body 없음. DB에 유저 없으면 생성, 있으면 name 업데이트 후 UserResponse 반환
    // /api/auth/** 는 permitAll이므로 이 엔드포인트 자체는 인증 없이 접근 가능하나,
    // oauth2ResourceServer가 Authorization 헤더의 Cognito JWT를 검증한 뒤 Jwt 객체를 주입
    @PostMapping("/social-login")
    public ResponseEntity<ApiResponse<UserResponse>> socialLoginWithCognito(
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("Cognito ID Token이 필요합니다.");
        }
        return ResponseEntity.ok(ApiResponse.ok(userService.socialLoginWithCognito(jwt)));
    }
}
