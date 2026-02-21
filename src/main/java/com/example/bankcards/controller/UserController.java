package com.example.bankcards.controller;

import com.example.bankcards.dto.PageResponseDto;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.mapping.UserMapper;
import com.example.bankcards.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public ResponseEntity<PageResponseDto<UserResponse>> getAllUsers(Pageable pageable) {
        Page<User> page = userService.getAllUsers(pageable);
        PageResponseDto<UserResponse> response = new
                PageResponseDto<>(
                page.getContent().stream().map(userMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userMapper.toResponse(userService.getUserById(id)));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<UserResponse> enableUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userMapper.toResponse(userService.enableUser(id)));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<UserResponse> disableUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userMapper.toResponse(userService.disableUser(id)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }


}
