package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.UserRequestDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.UserMapper;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.repository.UserRepository;
import com.quimia.quimiawebcrud.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService extends CrudService<User, Integer, UserRequestDTO, UserResponseDTO> {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordHasher passwordHasher;

    @Override
    protected UserRepository getRepository() {
        return repository;
    }

    @Override
    protected UserMapper getMapper() {
        return mapper;
    }

    @Override
    @Transactional
    public UserResponseDTO create(UserRequestDTO request) {
        if (!hasText(request.getPassword())) {
            throw new InvalidDataException("A senha é obrigatória.");
        }
        User entity = mapper.toEntity(request);
        entity.setPasswordHash(passwordHasher.hash(request.getPassword()));
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public UserResponseDTO update(Integer id, UserRequestDTO request) {
        User entity = getEntity(id);
        mapper.updateEntity(entity, request);
        if (hasText(request.getPassword())) {
            entity.setPasswordHash(passwordHasher.hash(request.getPassword()));
        }
        return mapper.toResponse(entity);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
