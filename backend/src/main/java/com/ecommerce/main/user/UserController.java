package com.ecommerce.main.user;

import com.ecommerce.main.admin.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication auth) {
        User user = findUser(auth.getName());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        User user = findUser(auth.getName());
        String name = body.get("name");
        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }
        return ResponseEntity.ok(UserResponse.from(userRepository.save(user)));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        User user = findUser(auth.getName());
        String current = body.get("currentPassword");
        String newPass  = body.get("newPassword");

        if (current == null || newPass == null || newPass.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Geçerli şifre ve en az 6 karakterli yeni şifre gereklidir."));
        }

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(current, user.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Mevcut şifre hatalı."));
        }

        user.setPasswordHash(passwordEncoder.encode(newPass));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Şifre başarıyla güncellendi."));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }
}
