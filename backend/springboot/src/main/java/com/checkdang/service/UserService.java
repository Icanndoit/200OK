package com.checkdang.service;

import com.checkdang.domain.RefreshToken;
import com.checkdang.domain.User;
import com.checkdang.dto.GuestConvertRequest;
import com.checkdang.dto.LoginRequest;
import com.checkdang.dto.SignupRequest;
import com.checkdang.dto.TokenResponse;
import com.checkdang.dto.UserResponse;
import com.checkdang.repository.RefreshTokenRepository;
import com.checkdang.repository.UserRepository;
import com.checkdang.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (!Boolean.TRUE.equals(request.getTermsAgreed())) {
            throw new IllegalArgumentException("이용약관에 동의해야 합니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole() != null ? request.getRole() : User.Role.PATIENT)
                .provider(User.Provider.LOCAL)
                .isGuest(false)
                .termsAgreed(true)
                .accountStatus(User.AccountStatus.ACTIVE)
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .height(request.getHeight())
                .weight(request.getWeight())
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = createRefreshToken(String.valueOf(user.getId()));

        return TokenResponse.of(accessToken, refreshToken, UserResponse.from(user));
    }

    @Transactional
    public TokenResponse guestConvert(GuestConvertRequest request) {
        if (!Boolean.TRUE.equals(request.getTermsAgreed())) {
            throw new IllegalArgumentException("이용약관에 동의해야 합니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole() != null ? request.getRole() : User.Role.PATIENT)
                .provider(User.Provider.LOCAL)
                .isGuest(false)
                .termsAgreed(true)
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        String accessToken = jwtTokenProvider.generateToken(savedUser.getEmail());
        String refreshToken = createRefreshToken(String.valueOf(savedUser.getId()));

        return TokenResponse.of(accessToken, refreshToken, UserResponse.from(savedUser));
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    public boolean checkEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public TokenResponse generateGuestToken() {
        String token = jwtTokenProvider.generateGuestToken();
        return TokenResponse.ofGuest(token);
    }

    // Cognito ID Token으로 소셜 로그인 처리
    // cognito:username 클레임으로 provider 판별, email 클레임으로 사용자 조회/생성
    // 팀원 백엔드 스펙 동일: DB에 없으면 INSERT, 있으면 name UPDATE 후 UserResponse 반환
    @Transactional
    public UserResponse socialLoginWithCognito(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Cognito 토큰에 email 클레임이 없습니다.");
        }

        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        User.Provider provider = resolveProvider(jwt);

        final String finalName = name;
        final User.Provider finalProvider = provider;

        User user = userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setName(finalName);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .name(finalName)
                        .provider(finalProvider)
                        .role(User.Role.PATIENT)
                        .isGuest(false)
                        .accountStatus(User.AccountStatus.ACTIVE)
                        .build()));

        return UserResponse.from(user);
    }

    // cognito:username prefix로 소셜 provider 판별
    // Google_xxx → GOOGLE, KakaoOIDC_xxx → KAKAO, 그 외 → LOCAL
    private User.Provider resolveProvider(Jwt jwt) {
        String username = jwt.getClaimAsString("cognito:username");
        if (username != null) {
            if (username.startsWith("Google_")) return User.Provider.GOOGLE;
            if (username.startsWith("KakaoOIDC_")) return User.Provider.KAKAO;
        }
        return User.Provider.LOCAL;
    }

    private String createRefreshToken(String userId) {
        String token = UUID.randomUUID().toString();
        long expiresAt = Instant.now().getEpochSecond() + (refreshExpiration / 1000);

        refreshTokenRepository.save(RefreshToken.builder()
                .token(token)
                .userId(userId)
                .expiresAt(expiresAt)
                .createdAt(Instant.now().toString())
                .build());

        return token;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .roles(user.getRole().name())
                .build();
    }
}
