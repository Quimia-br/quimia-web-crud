package com.quimia.quimiawebcrud.service.crud;

import com.quimia.quimiawebcrud.dto.request.CompanyRequestDTO;
import com.quimia.quimiawebcrud.dto.response.CompanyResponseDTO;
import com.quimia.quimiawebcrud.exception.DuplicateResourceException;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.CompanyMapper;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import com.quimia.quimiawebcrud.security.PasswordHasher;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.EditLog;
import com.quimia.quimiawebcrud.service.photo.PhotoStorageService;
import com.quimia.quimiawebcrud.service.removal.RecordRemovalService;
import com.quimia.quimiawebcrud.service.verification.ExternalChecks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de empresas: validação, gravação, remoção e registro no log de edições.
 */
@Service
@RequiredArgsConstructor
public class CompanyService extends CrudService<Company, Integer, CompanyRequestDTO, CompanyResponseDTO> {

    private final CompanyRepository repository;
    private final CompanyMapper mapper;
    private final PasswordHasher passwordHasher;
    private final PhotoStorageService photoStorage;
    private final RecordRemovalService removal;
    private final EditLog editLog;
    private final ExternalChecks checks;

    @Override
    protected CompanyRepository getRepository() {
        return repository;
    }

    @Override
    protected CompanyMapper getMapper() {
        return mapper;
    }

    @Override
    protected EditLog getEditLog() {
        return editLog;
    }

    /** E-mail (domínio) e CNPJ precisam existir; só são conferidos quando mudaram. */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void verifyBeforeSave(CompanyRequestDTO request, Integer id) {
        Company current = id == null ? null : repository.findById(id).orElse(null);
        checks.requireEmailDomain(request.getEmail(), current == null ? null : current.getEmail());
        checks.requireCnpj(request.getCnpj(), current == null ? null : current.getCnpj());
    }

    @Override
    protected void validate(CompanyRequestDTO request, Integer id) {
        if (id == null && !Passwords.present(request.getPassword())) {
            throw new InvalidDataException("A senha é obrigatória.");
        }
        boolean emailTaken = id == null
                ? repository.existsByEmailIgnoreCase(request.getEmail())
                : repository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), id);
        if (emailTaken) {
            throw new DuplicateResourceException("Já existe uma empresa com este e-mail.");
        }
        boolean cnpjTaken = id == null
                ? repository.existsByCnpj(request.getCnpj())
                : repository.existsByCnpjAndIdNot(request.getCnpj(), id);
        if (cnpjTaken) {
            throw new DuplicateResourceException("Já existe uma empresa com este CNPJ.");
        }
    }

    @Override
    protected Company newEntity(CompanyRequestDTO request) {
        Company entity = mapper.toEntity(request);
        entity.setPasswordHash(passwordHasher.hash(request.getPassword()));
        entity.setPhotoUrl(photoStorage.replace(request.getPhoto(), null));
        return entity;
    }

    @Override
    protected void applyChanges(Company entity, CompanyRequestDTO request) {
        String currentPhoto = entity.getPhotoUrl();
        mapper.updateEntity(entity, request);
        entity.setPhotoUrl(photoStorage.replace(request.getPhoto(), currentPhoto));
        if (Passwords.present(request.getPassword())) {
            entity.setPasswordHash(passwordHasher.hash(request.getPassword()));
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Company record = getEntity(id);
        String photoUrl = record.getPhotoUrl();
        removal.removeCompany(record);
        photoStorage.release(photoUrl);
    }
}
