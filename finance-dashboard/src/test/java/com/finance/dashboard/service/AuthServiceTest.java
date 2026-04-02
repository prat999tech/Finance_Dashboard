package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.LoginRequest;
import com.finance.dashboard.dto.request.RegisterRequest;
import com.finance.dashboard.dto.response.JwtResponse;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.exception.DuplicateResourceException;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private UserDetails sampleUserDetails;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Alice Admin")
                .email("admin@finance.com")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        sampleUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("admin@finance.com")
                .password("encodedPassword")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: success creates user and returns JWT")
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice Admin");
        request.setEmail("admin@finance.com");
        request.setPassword("password123");
        request.setRole(Role.ADMIN);

        when(userRepository.existsByEmail("admin@finance.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userDetailsService.loadUserByUsername("admin@finance.com")).thenReturn(sampleUserDetails);
        when(jwtUtil.generateToken(sampleUserDetails)).thenReturn("mock.jwt.token");

        JwtResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getEmail()).isEqualTo("admin@finance.com");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("register: throws DuplicateResourceException when email already exists")
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@finance.com");
        request.setPassword("pass");
        request.setRole(Role.VIEWER);

        when(userRepository.existsByEmail("admin@finance.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("admin@finance.com");

        verify(userRepository, never()).save(any());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: success returns JWT for valid credentials")
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@finance.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // authenticate() returns Authentication, null is fine for test
        when(userRepository.findByEmail("admin@finance.com")).thenReturn(Optional.of(sampleUser));
        when(userDetailsService.loadUserByUsername("admin@finance.com")).thenReturn(sampleUserDetails);
        when(jwtUtil.generateToken(sampleUserDetails)).thenReturn("mock.jwt.token");

        JwtResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getEmail()).isEqualTo("admin@finance.com");
    }

    @Test
    @DisplayName("login: throws BadCredentialsException for wrong password")
    void login_wrongPassword_throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@finance.com");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
