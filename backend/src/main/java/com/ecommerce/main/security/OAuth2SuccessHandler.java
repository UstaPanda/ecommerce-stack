package com.ecommerce.main.security;

import com.ecommerce.main.token.RefreshToken;
import com.ecommerce.main.token.RefreshTokenRepository;
import com.ecommerce.main.user.AuthProvider;
import com.ecommerce.main.user.Role;
import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String registrationId = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication)
                .getAuthorizedClientRegistrationId();

        User user;
        if ("facebook".equals(registrationId)) {
            user = handleFacebook(oAuth2User);
        } else {
            user = handleGoogle(oAuth2User);
        }

        refreshTokenRepository.revokeAllByUserId(user.getId());

        String accessToken = jwtService.generateToken(user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);

        String redirectUrl = String.format(
                "http://localhost:4200/auth/oauth2/callback?accessToken=%s&refreshToken=%s&email=%s&name=%s&role=%s",
                accessToken,
                refreshToken.getToken(),
                java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(user.getName(), java.nio.charset.StandardCharsets.UTF_8),
                user.getRoleType()
        );
        response.sendRedirect(redirectUrl);
    }

    private User handleGoogle(OAuth2User oAuth2User) {
        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");

        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(
            User.builder()
                .email(email).name(name).googleId(googleId)
                .provider(AuthProvider.GOOGLE)
                .roleType(Role.INDIVIDUAL).verified(true)
                .build()
        ));
    }

    private User handleFacebook(OAuth2User oAuth2User) {
        String facebookId = oAuth2User.getAttribute("id");
        String name       = oAuth2User.getAttribute("name");
        String email      = oAuth2User.getAttribute("email");

        // Facebook bazen email vermeyebilir — fallback olarak fb ID'den üret
        if (email == null || email.isBlank()) {
            email = facebookId + "@facebook-noemail.local";
        }

        final String finalEmail = email;
        final String finalFbId  = facebookId;
        final String finalName  = name;

        return userRepository.findByEmail(finalEmail).orElseGet(() -> userRepository.save(
            User.builder()
                .email(finalEmail).name(finalName).facebookId(finalFbId)
                .provider(AuthProvider.FACEBOOK)
                .roleType(Role.INDIVIDUAL).verified(true)
                .build()
        ));
    }
}
