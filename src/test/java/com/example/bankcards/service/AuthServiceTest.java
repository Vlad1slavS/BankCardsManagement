package com.example.bankcards.service;

import com.example.bankcards.dto.AuthResponse;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.UserRole;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User user;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("Ivan Ivanov")
                .email("ivan@test.com")
                .password("encodedPass")
                .role(UserRole.USER)
                .enabled(true)
                .build();

        userPrincipal = new UserPrincipal(user);
    }

    @Test
    void login_success_returnsToken() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userPrincipal);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken(userPrincipal)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("Ivan Ivanov", "pass"));

        assertEquals("jwt-token", response.token());
        assertEquals("Ivan Ivanov", response.username());
        assertEquals("ROLE_USER", response.role());
    }

    @Test
    void login_wrongCredentials_throwsBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("Ivan Ivanov", "wrong")));
    }

    @Test
    void register_success_returnsToken() {
        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        RegisterRequest request = new RegisterRequest("newUser", "new@test.com", "Password1".toCharArray());
        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.token());
        assertEquals(UserRole.USER.name(), response.role());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throwsDuplicateResourceException() {
        when(userRepository.existsByUsername("Ivan Ivanov")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("Ivan Ivanov", "other@test.com", "Password1".toCharArray());

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("ivan@test.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("newUser", "ivan@test.com", "pass".toCharArray());

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_clearsPasswordArray() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        char[] password = "Password1".toCharArray();
        authService.register(new RegisterRequest("user", "u@test.com", password));

        for (char c : password) {
            assertEquals('\0', c);
        }
    }
}
