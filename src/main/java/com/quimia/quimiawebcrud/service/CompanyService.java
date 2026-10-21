package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.CompanyRequestDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.CompanyMapper;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import com.quimia.quimiawebcrud.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService extends CrudService<Company, Integer, CompanyRequestDTO, CompanyResponseDTO> {

    private final CompanyRepository repository;
    private final CompanyMapper mapper;
    private final PasswordHasher passwordHasher;
    private final PhotoStorageService photoStorage;
    private final RecordRemovalService removal;

    @Override
    protected CompanyRepository getRepository() {
        return repository;
    }

    @Override
    protected CompanyMapper getMapper() {
        return mapper;
    }

    @Override
    @Transactional
    public CompanyResponseDTO create(CompanyRequestDTO request) {
        if (!hasText(request.getPassword())) {
            throw new InvalidDataException("A senha é obrigatória.");
        }
        Company entity = mapper.toEntity(request);
        entity.setPasswordHash(passwordHasher.hash(request.getPassword()));
        entity.setPhotoUrl(photoStorage.replace(request.getPhoto(), null));
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public CompanyResponseDTO update(Integer id, CompanyRequestDTO request) {
        Company entity = getEntity(id);
        String currentPhoto = entity.getPhotoUrl();
        mapper.updateEntity(entity, request);
        entity.setPhotoUrl(photoStorage.replace(request.getPhoto(), currentPhoto));
        if (hasText(request.getPassword())) {
            entity.setPasswordHash(passwordHasher.hash(request.getPassword()));
        }
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Company record = getEntity(id);
        String photoUrl = record.getPhotoUrl();
        removal.removeCompany(record);
        photoStorage.release(photoUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
