package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.UserRequestDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.mapper.UserMapper;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mindrot.jbcrypt.BCrypt;

@Service
@RequiredArgsConstructor
public class UserService extends CrudService<User, Integer, UserRequestDTO, UserResponseDTO> {

    private final UserRepository repository;
    private final UserMapper mapper;

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
            throw new IllegalArgumentException("A senha e obrigatoria.");
        }
        User entity = mapper.toEntity(request);
        entity.setPasswordHash(hashPassword(request.getPassword()));
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public UserResponseDTO update(Integer id, UserRequestDTO request) {
        User entity = getEntity(id);
        mapper.updateEntity(entity, request);
        if (hasText(request.getPassword())) {
            entity.setPasswordHash(hashPassword(request.getPassword()));
        }
        return mapper.toResponse(entity);
    }

    private String hashPassword(String originalPassword) {
        return BCrypt.hashpw(originalPassword, BCrypt.gensalt());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
