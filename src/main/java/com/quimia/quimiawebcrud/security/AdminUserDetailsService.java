package com.quimia.quimiawebcrud.security;

import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Busca o administrador pelo e-mail para o login. Só entra quem está com status ATIVO e não está bloqueado
 * por tentativas erradas; o motivo da recusa nunca é mostrado na tela.
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final LoginAttemptService loginAttempts;

    @Override
    public UserDetails loadUserByUsername(String email) {
        Admin admin = adminRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Administrador não encontrado"));
        return User.withUsername(admin.getEmail())
                .password(admin.getPasswordHash())
                .roles("ADMIN")
                .disabled(admin.getStatus() != Status.ATIVO)
                .accountLocked(loginAttempts.isBlocked(admin.getEmail()))
                .build();
    }
}
