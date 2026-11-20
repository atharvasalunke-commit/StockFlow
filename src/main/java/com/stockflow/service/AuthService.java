package com.stockflow.service;

import java.util.Locale;

import com.stockflow.auth.EmailAlreadyUsedException;
import com.stockflow.auth.InvalidCredentialsException;
import com.stockflow.dto.LoginRequest;
import com.stockflow.dto.RegisterRequest;
import com.stockflow.dto.TokenResponse;
import com.stockflow.dto.UserResponse;
import com.stockflow.entity.User;
import com.stockflow.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim());
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyUsedException();
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = normalize(request.email());
        User user = userRepository.findByEmail(email).orElse(null);
        String hashToCheck = (user != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = passwordEncoder.matches(request.password(), hashToCheck);
        if (user == null || !passwordOk) {
            throw new InvalidCredentialsException();
        }

        TokenService.IssuedToken token = tokenService.issue(user.getId());
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
