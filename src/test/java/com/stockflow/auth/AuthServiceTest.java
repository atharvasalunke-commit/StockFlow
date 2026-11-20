package com.stockflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.stockflow.service.AuthService;
import com.stockflow.service.TokenService;
import com.stockflow.dto.LoginRequest;
import com.stockflow.dto.RegisterRequest;
import com.stockflow.dto.TokenResponse;
import com.stockflow.entity.User;
import com.stockflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    TokenService tokenService;

    PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, encoder, tokenService);
    }

    @Test
    void register_normalizesEmail_andStoresHashNotPassword() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(new RegisterRequest("  A@B.com ", "password123", " Atharva "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("a@b.com");
        assertThat(saved.getValue().getFullName()).isEqualTo("Atharva");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("password123");
        assertThat(encoder.matches("password123", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void register_duplicateEmail_throws_andDoesNotSave() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(new RegisterRequest("a@b.com", "password123", "A")))
                .isInstanceOf(EmailAlreadyUsedException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void login_wrongPassword_throwsGenericError() {
        User user = new User("a@b.com", encoder.encode("password123"), "A");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throwsSameGenericError() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("x@y.com", "whatever123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_success_returnsBearerToken() {
        User user = new User("a@b.com", encoder.encode("password123"), "A");
        ReflectionTestUtils.setField(user, "id", 7L);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(tokenService.issue(7L)).thenReturn(new TokenService.IssuedToken("jwt-value", 3600));
        TokenResponse response = authService.login(new LoginRequest("A@b.com", "password123"));
        assertThat(response.accessToken()).isEqualTo("jwt-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
    }
}
