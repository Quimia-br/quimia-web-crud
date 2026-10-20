package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.UserRequestDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper implements Mapper<User, UserRequestDTO, UserResponseDTO> {

    @Override
    public User toEntity(UserRequestDTO request) {
        if (request == null) {
            return null;
        }
        return User.builder()
                .userName(request.getUserName())
                .birthDate(request.getBirthDate())
                .email(request.getEmail())
                .status(request.getStatus())
                .photoUrl(request.getPhotoUrl())
                .build();
    }

    @Override
    public UserResponseDTO toResponse(User entity) {
        if (entity == null) {
            return null;
        }
        return UserResponseDTO.builder()
                .id(entity.getId())
                .userName(entity.getUserName())
                .birthDate(entity.getBirthDate())
                .email(entity.getEmail())
                .signUpDate(entity.getSignUpDate())
                .status(entity.getStatus())
                .photoUrl(entity.getPhotoUrl())
                .build();
    }

    @Override
    public void updateEntity(User entity, UserRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUserName(request.getUserName());
        entity.setBirthDate(request.getBirthDate());
        entity.setEmail(request.getEmail());
        entity.setPhotoUrl(request.getPhotoUrl());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }
}
