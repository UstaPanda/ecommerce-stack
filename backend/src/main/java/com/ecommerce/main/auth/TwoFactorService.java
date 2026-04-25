package com.ecommerce.main.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.main.security.JwtService;
import com.ecommerce.main.token.RefreshToken;
import com.ecommerce.main.token.RefreshTokenRepository;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import static dev.samstevens.totp.util.Utils.getDataUriForImage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public TwoFactorSetupResponse setup(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));

        if (user.isTwoFactorEnabled()) {
            throw new IllegalStateException("2FA zaten aktif");
        }

        String secret = new DefaultSecretGenerator().generate();
        user.setTotpSecret(secret);
        userRepository.save(user);

        QrData qrData = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer("Ecommerce App")
                .build();

        try {
            QrGenerator qrGenerator = new ZxingPngQrGenerator();
            byte[] imageData = qrGenerator.generate(qrData);
            String qrCodeDataUri = getDataUriForImage(imageData, qrGenerator.getImageMimeType());
            return new TwoFactorSetupResponse(secret, qrCodeDataUri);
        } catch (Exception e) {
            throw new RuntimeException("QR kod olusturulamadi");
        }
    }

    @Transactional
    public String confirm(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));

        if (user.getTotpSecret() == null) {
            throw new IllegalStateException("Once 2FA kurulumu baslatilmali");
        }

        if (!verifyCode(user.getTotpSecret(), code)) {
            throw new IllegalArgumentException("Gecersiz kod");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        return "2FA basariyla aktif edildi.";
    }

    @Transactional
    public String disable(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));

        if (!user.isTwoFactorEnabled()) {
            throw new IllegalStateException("2FA zaten devre disi");
        }

        if (!verifyCode(user.getTotpSecret(), code)) {
            throw new IllegalArgumentException("Gecersiz kod");
        }

        user.setTwoFactorEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);

        return "2FA devre disi birakildi.";
    }

    @Transactional
    public AuthResponse validate(String tempToken, String code, String deviceName, String ipAddress) {
        if (!jwtService.isPending2faToken(tempToken)) {
            throw new IllegalArgumentException("Gecersiz token");
        }

        String email = jwtService.extractEmail(tempToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));

        if (!verifyCode(user.getTotpSecret(), code)) {
            throw new IllegalArgumentException("Gecersiz 2FA kodu");
        }

        refreshTokenRepository.revokeAllByUserId(user.getId());

        String accessToken = jwtService.generateToken(email);
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .deviceName(deviceName)
                .ipAddress(ipAddress)
                .lastSeen(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .role(user.getRoleType().name())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    private boolean verifyCode(String secret, String code) {
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());
        return verifier.isValidCode(secret, code);
    }
}
