package com.quimia.quimiawebcrud.config;

import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Todos os repositórios usam {@link ColumnFilterRepositoryImpl} como base, para ganhar os métodos por coluna (busca, atualização e exclusão).
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.quimia.quimiawebcrud.repository", repositoryBaseClass = ColumnFilterRepositoryImpl.class)
public class JpaConfig {
}
