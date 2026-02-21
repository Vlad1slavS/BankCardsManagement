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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtUtil.generateToken(principal);

        String role = principal.getAuthorities().iterator().next().getAuthority();

        return AuthResponse.builder()
                .token(token)
                .username(principal.getUsername())
                .role(role)
                .build();

    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException(
                    "Имя пользователя '" + request.username() + "' занято");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                    "Почта '" + request.email() + "' занята");
        }

        String encoded = passwordEncoder.encode(new String(request.password()));
        Arrays.fill(request.password(), '\0');

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(encoded)
                .role(UserRole.USER)
                .enabled(true)
                .build();

        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtUtil.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(UserRole.USER.name())
                .build();
    }

}
