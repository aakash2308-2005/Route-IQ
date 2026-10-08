package com.smartcommute.application.user;

import com.smartcommute.persistence.entity.UserEntity;
import com.smartcommute.persistence.repository.UserRepository;
import com.smartcommute.security.JwtService;
import com.smartcommute.web.error.ConflictException;
import com.smartcommute.web.error.UnauthorizedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User registration and authentication (docs/03-hld.md section 2.2).
 *
 * <p>Passwords are hashed with BCrypt before storage (NFR-9). Login verifies the hash and issues a
 * JWT. The login failure message is intentionally generic ("invalid credentials") so it does not
 * reveal whether the email exists.</p>
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserEntity register(String email, String rawPassword, String displayName) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("email already registered");
        }
        UserEntity user = new UserEntity(email, passwordEncoder.encode(rawPassword), displayName);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Defends against a race between the existsByEmail check and insert.
            throw new ConflictException("email already registered");
        }
    }

    /** Returns a freshly issued token result on success; throws {@link UnauthorizedException} otherwise. */
    @Transactional(readOnly = true)
    public AuthResult authenticate(String email, String rawPassword) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }
        String token = jwtService.issue(user.getId(), user.getEmail());
        return new AuthResult(token, jwtService.expirySeconds(), user.getId());
    }

    public record AuthResult(String token, long expiresInSeconds, long userId) {
    }
}
