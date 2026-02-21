package com.example.bankcards.service;

import com.example.bankcards.entity.User;
import com.example.bankcards.enums.UserRole;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("test@test.com")
                .role(UserRole.USER)
                .password("testPass")
                .username("testUser")
                .enabled(true)
                .build();
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getUserById(userId);

        assertEquals("testUser", result.getUsername());
        assertEquals("test@test.com", result.getEmail());
    }

    @Test
    void getUserById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(userId));
    }

    @Test
    void getAllUsers_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        Page<User> result = userService.getAllUsers(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("testUser", result.getContent().getFirst().getUsername());
    }

    @Test
    void getAllUsers_emptyPage_returnsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        Page<User> result = userService.getAllUsers(pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void enableUser_success() {
        user.setEnabled(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        User result = userService.enableUser(userId);

        assertTrue(result.isEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void enableUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.enableUser(userId));
        verify(userRepository, never()).save(any());
    }

    @Test
    void disableUser_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        User result = userService.disableUser(userId);

        assertFalse(result.isEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void disableUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.disableUser(userId));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_success_setsDeletedAt() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.deleteUser(userId));

        assertNotNull(user.getDeletedAt());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(userId));
    }
}