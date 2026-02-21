package com.example.bankcards.mapping;

import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
