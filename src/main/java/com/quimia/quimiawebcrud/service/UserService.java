package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.UserRequestDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.mapper.UserMapper;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;

    private String hashPassword(String originalPassword) {
        return BCrypt.hashpw(originalPassword, BCrypt.gensalt());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public List<UserResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public UserResponseDTO findById(Integer id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public UserResponseDTO create(UserRequestDTO request) {
        if (!hasText(request.getPassword())) {
            throw new IllegalArgumentException("A senha e obrigatoria.");
        }
        User entity = mapper.toEntity(request);
        entity.setPasswordHash(hashPassword(request.getPassword()));
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public UserResponseDTO update(Integer id, UserRequestDTO request) {
        User entity = getEntity(id);
        mapper.updateEntity(entity, request);
        if (hasText(request.getPassword())) {
            entity.setPasswordHash(hashPassword(request.getPassword()));
        }
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(getEntity(id));
    }

    private User getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado com o ID: " + id));
    }
}
