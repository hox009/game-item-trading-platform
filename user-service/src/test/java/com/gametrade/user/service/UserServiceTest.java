package com.gametrade.user.service;

import com.gametrade.common.exception.BusinessException;
import com.gametrade.common.security.JwtUtil;
import com.gametrade.user.domain.Role;
import com.gametrade.user.domain.User;
import com.gametrade.user.dto.LoginRequest;
import com.gametrade.user.dto.LoginResponse;
import com.gametrade.user.dto.RegisterRequest;
import com.gametrade.user.dto.UserProfileResponse;
import com.gametrade.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtil = new JwtUtil("change-me-please-use-a-32-byte-secret-key!!", 7_200_000L);
        userService = new UserService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void register_persistsUserWithHashedPassword() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserProfileResponse response = userService.register(new RegisterRequest("alice", "secret123", "SELLER"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.role()).isEqualTo("SELLER");
    }

    @Test
    void register_rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new RegisterRequest("bob", "secret123", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        User user = new User();
        user.setId(7L);
        user.setUsername("carol");
        user.setRole(Role.BUYER);
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));

        LoginResponse response = userService.login(new LoginRequest("carol", "secret123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(jwtUtil.isValid(response.token())).isTrue();
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = new User();
        user.setId(7L);
        user.setUsername("carol");
        user.setRole(Role.BUYER);
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(new LoginRequest("carol", "wrongpass")))
                .isInstanceOf(BusinessException.class);
    }
}
