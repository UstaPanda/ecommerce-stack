package com.ecommerce.main.auth;

import com.ecommerce.main.audit.AuditEventType;
import com.ecommerce.main.audit.AuditLogService;
import com.ecommerce.main.email.EmailService;
import com.ecommerce.main.security.JwtService;
import com.ecommerce.main.token.RefreshToken;
import com.ecommerce.main.token.RefreshTokenRepository;
import com.ecommerce.main.user.Role;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    @Value("${email.verification.enabled:true}")
    private boolean emailVerificationEnabled;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String register(RegisterRequest request) {
        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin olarak kayit yapilamaz");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            // Email var olduğunu açıkça belirtmiyoruz (user enumeration güvenlik açığı)
            // Gerçek kullanıcıya uyarı maili gönderip aynı cevabı dönüyoruz
            emailService.sendRegistrationAttemptEmail(request.getEmail());
            auditLogService.log(AuditEventType.REGISTER_EMAIL_CONFLICT, request.getEmail(), null, null, "Mevcut email ile kayit girişimi");
            return emailVerificationEnabled
                    ? "Kayit basarili. Lutfen emailinizi dogrulayin."
                    : "Kayit basarili.";
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roleType(request.getRole())
                .build();

        if (emailVerificationEnabled) {
            userRepository.save(user);
            sendVerificationCode(user);
            auditLogService.log(AuditEventType.REGISTER, request.getEmail(), null, null);
            return "Kayit basarili. Lutfen emailinizi dogrulayin.";
        }

        user.setVerified(true);
        userRepository.save(user);
        auditLogService.log(AuditEventType.REGISTER, request.getEmail(), null, null);
        return "Kayit basarili.";
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceName, String ipAddress) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Gecersiz email veya sifre"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Hesabiniz gecici olarak kilitlendi. Lutfen " + LOCK_DURATION_MINUTES + " dakika sonra tekrar deneyin.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            }
            userRepository.save(user);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                auditLogService.log(AuditEventType.ACCOUNT_LOCKED, request.getEmail(), ipAddress, deviceName, "5 basarisiz giris denemesi");
            }
            auditLogService.log(AuditEventType.LOGIN_FAILED, request.getEmail(), ipAddress, deviceName);
            throw new BadCredentialsException("Gecersiz email veya sifre");
        }

        if (emailVerificationEnabled && !user.isVerified()) {
            throw new IllegalStateException("Lutfen once emailinizi dogrulayin");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        if (user.isTwoFactorEnabled()) {
            String tempToken = jwtService.generatePending2faToken(user.getEmail());
            auditLogService.log(AuditEventType.LOGIN_SUCCESS, user.getEmail(), ipAddress, deviceName, "2FA gerekli");
            return AuthResponse.builder()
                    .requiresTwoFactor(true)
                    .tempToken(tempToken)
                    .build();
        }

        refreshTokenRepository.revokeAllByUserId(user.getId());

        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshTokenValue = UUID.randomUUID().toString();

        int tokenDays = request.isRememberMe() ? 30 : 1;
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusDays(tokenDays))
                .deviceName(deviceName)
                .ipAddress(ipAddress)
                .lastSeen(LocalDateTime.now())
                .rememberMe(request.isRememberMe())
                .build();

        refreshTokenRepository.save(refreshToken);

        auditLogService.log(AuditEventType.LOGIN_SUCCESS, user.getEmail(), ipAddress, deviceName);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .role(user.getRoleType().name())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    public java.util.List<DeviceResponse> getDevices(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));
        return refreshTokenRepository.findByUserAndRevokedFalse(user).stream()
                .filter(rt -> rt.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(rt -> new DeviceResponse(rt.getId(), rt.getDeviceName(), rt.getIpAddress(), rt.getLastSeen()))
                .toList();
    }

    @Transactional
    public String revokeDevice(String email, Long deviceId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));
        RefreshToken token = refreshTokenRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Oturum bulunamadi"));
        if (!token.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bu oturum size ait degil");
        }
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return "Oturum sonlandirildi.";
    }

    @Transactional
    public String verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new IllegalArgumentException("Gecersiz dogrulama kodu");
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Dogrulama kodunun suresi dolmus");
        }

        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        auditLogService.log(AuditEventType.EMAIL_VERIFIED, email, null, null);
        return "Email dogrulandi. Artik giris yapabilirsiniz.";
    }

    @Transactional
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Bu email ile kayitli kullanici bulunamadi"));

        String resetCode = String.format("%06d", secureRandom.nextInt(1000000));
        user.setPasswordResetCode(resetCode);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(email, resetCode);

        auditLogService.log(AuditEventType.PASSWORD_RESET_REQUEST, email, null, null);
        return "Sifre sifirlama kodu emailinize gonderildi.";
    }

    @Transactional
    public String resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));

        if (user.getPasswordResetCode() == null || !user.getPasswordResetCode().equals(code)) {
            throw new IllegalArgumentException("Gecersiz sifre sifirlama kodu");
        }

        if (user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Sifre sifirlama kodunun suresi dolmus");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetCode(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);

        auditLogService.log(AuditEventType.PASSWORD_RESET_SUCCESS, email, null, null);
        return "Sifreniz basariyla guncellendi.";
    }

    @Transactional
    public String resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));

        if (user.isVerified()) {
            throw new IllegalArgumentException("Bu hesap zaten dogrulanmis");
        }

        sendVerificationCode(user);

        return "Yeni dogrulama kodu gonderildi.";
    }

    private void sendVerificationCode(User user) {
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), code);
    }

    @Transactional
    public String logout(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Gecersiz refresh token"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        auditLogService.log(AuditEventType.LOGOUT, refreshToken.getUser().getEmail(), null, null);
        return "Cikis yapildi.";
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Gecersiz refresh token"));

        if (refreshToken.isRevoked()) {
            throw new IllegalArgumentException("Refresh token iptal edilmis");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token suresi dolmus");
        }

        User user = refreshToken.getUser();

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtService.generateToken(user.getEmail());
        String newRefreshTokenValue = UUID.randomUUID().toString();

        int tokenDays = refreshToken.isRememberMe() ? 30 : 1;
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(newRefreshTokenValue)
                .expiresAt(LocalDateTime.now().plusDays(tokenDays))
                .rememberMe(refreshToken.isRememberMe())
                .build();

        refreshTokenRepository.save(newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .role(user.getRoleType().name())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
