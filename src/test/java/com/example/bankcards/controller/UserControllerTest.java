package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.UserRole;
import com.example.bankcards.mapping.UserMapper;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    private UUID userId;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("Ivan Ivanov");
        user.setEmail("ivan@example.com");
        user.setEnabled(true);

        userResponse = UserResponse.builder()
                .id(userId)
                .username("Ivan Ivanov")
                .email("ivan@example.com")
                .role(UserRole.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_admin_returns200() throws Exception {
        when(userService.getAllUsers(any()))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].username").value("Ivan Ivanov"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_user_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUser_admin_returns200() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("Ivan Ivanov"))
                .andExpect(jsonPath("$.email").value("ivan@example.com"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUser_user_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void enableUser_admin_returns200() throws Exception {
        when(userService.enableUser(userId)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        mockMvc.perform(patch("/api/v1/users/{id}/enable", userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void enableUser_user_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/enable", userId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void disableUser_admin_returns200() throws Exception {
        UserResponse disabled = UserResponse.builder()
                .id(userId)
                .username("Ivan Ivanov")
                .email("ivan@example.com")
                .role(UserRole.USER)
                .enabled(false)
                .createdAt(LocalDateTime.now())
                .build();

        User disabledUser = new User();
        disabledUser.setId(userId);
        disabledUser.setEnabled(false);

        when(userService.disableUser(userId)).thenReturn(disabledUser);
        when(userMapper.toResponse(disabledUser)).thenReturn(disabled);

        mockMvc.perform(patch("/api/v1/users/{id}/disable", userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void disableUser_user_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/disable", userId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_admin_returns204() throws Exception {
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/users/{id}", userId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_user_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", userId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", userId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
    
}
