package com.quimia.quimiawebcrud.service.crud;

import com.quimia.quimiawebcrud.dto.request.UserRequestDTO;
import com.quimia.quimiawebcrud.dto.response.UserResponseDTO;
import com.quimia.quimiawebcrud.exception.DuplicateResourceException;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.UserMapper;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.repository.UserRepository;
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
 * Regras de usuários: validação, gravação, remoção e registro no log de edições.
 */
@Service
@RequiredArgsConstructor
public class UserService extends CrudService<User, Integer, UserRequestDTO, UserResponseDTO> {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordHasher passwordHasher;
    private final PhotoStorageService photoStorage;
    private final RecordRemovalService removal;
    private final EditLog editLog;
    private final ExternalChecks checks;

    @Override
    protected UserRepository getRepository() {
        return repository;
    }

    @Override
    protected UserMapper getMapper() {
        return mapper;
    }

    @Override
    protected EditLog getEditLog() {
        return editLog;
    }

    /** O domínio do e-mail precisa existir; só é conferido quando o e-mail mudou. */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void verifyBeforeSave(UserRequestDTO request, Integer id) {
        User current = id == null ? null : repository.findById(id).orElse(null);
        checks.requireEmailDomain(request.getEmail(), current == null ? null : current.getEmail());
    }

    @Override
    protected void validate(UserRequestDTO request, Integer id) {
        if (id == null && !Passwords.present(request.getPassword())) {
            throw new InvalidDataException("A senha é obrigatória.");
        }
        boolean emailTaken = id == null
                ? repository.existsByEmailIgnoreCase(request.getEmail())
                : repository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), id);
        if (emailTaken) {
            throw new DuplicateResourceException("Já existe um usuário com este e-mail.");
        }
    }

    @Override
    protected User newEntity(UserRequestDTO request) {
        User entity = mapper.toEntity(request);
        entity.setPasswordHash(passwordHasher.hash(request.getPassword()));
        entity.setPhotoUrl(photoStorage.replace(request.getPhoto(), null));
        return entity;
    }

    @Override
    protected void applyChanges(User entity, UserRequestDTO request) {
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
        User record = getEntity(id);
        String photoUrl = record.getPhotoUrl();
        removal.removeUser(record);
        photoStorage.release(photoUrl);
    }
}
