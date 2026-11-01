package com.quimia.quimiawebcrud.service.crud;

import com.quimia.quimiawebcrud.dto.AdminRequestDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.exception.DuplicateResourceException;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.AdminMapper;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import com.quimia.quimiawebcrud.security.PasswordHasher;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.CurrentAdmin;
import com.quimia.quimiawebcrud.service.EditLog;
import com.quimia.quimiawebcrud.service.removal.RecordRemovalService;
import com.quimia.quimiawebcrud.service.verification.ExternalChecks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de administradores: validação, gravação, remoção e registro no log de edições.
 */
@Service
@RequiredArgsConstructor
public class AdminService extends CrudService<Admin, Integer, AdminRequestDTO, AdminResponseDTO> {

    private final AdminRepository repository;
    private final AdminMapper mapper;
    private final PasswordHasher passwordHasher;
    private final RecordRemovalService removal;
    private final EditLog editLog;
    private final CurrentAdmin currentAdmin;
    private final ExternalChecks checks;

    @Override
    protected AdminRepository getRepository() {
        return repository;
    }

    @Override
    protected AdminMapper getMapper() {
        return mapper;
    }

    @Override
    protected EditLog getEditLog() {
        return editLog;
    }

    /** O domínio do e-mail precisa existir; só é conferido quando o e-mail mudou. */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void verifyBeforeSave(AdminRequestDTO request, Integer id) {
        Admin current = id == null ? null : repository.findById(id).orElse(null);
        checks.requireEmailDomain(request.getEmail(), current == null ? null : current.getEmail());
    }

    /**
     * Só administradores ativos entram no sistema, então ninguém pode tirar a própria conta de "ativo"
     * (ficaria trancado para fora).
     */
    @Override
    protected void validate(AdminRequestDTO request, Integer id) {
        if (id == null && !Passwords.present(request.getPassword())) {
            throw new InvalidDataException("A senha é obrigatória.");
        }
        if (id != null && request.getStatus() != Status.ATIVO && id.equals(currentAdmin.require().getId())) {
            throw new InvalidDataException("Você não pode mudar o status da sua própria conta.");
        }
        boolean emailTaken = id == null
                ? repository.existsByEmailIgnoreCase(request.getEmail())
                : repository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), id);
        if (emailTaken) {
            throw new DuplicateResourceException("Já existe um administrador com este e-mail.");
        }
    }

    @Override
    protected Admin newEntity(AdminRequestDTO request) {
        Admin entity = mapper.toEntity(request);
        entity.setPasswordHash(passwordHasher.hash(request.getPassword()));
        return entity;
    }

    @Override
    protected void applyChanges(Admin entity, AdminRequestDTO request) {
        mapper.updateEntity(entity, request);
        if (Passwords.present(request.getPassword())) {
            entity.setPasswordHash(passwordHasher.hash(request.getPassword()));
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        removal.removeAdmin(getEntity(id));
    }
}
