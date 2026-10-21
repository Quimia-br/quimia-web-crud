package com.quimia.quimiawebcrud.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * As consultas de exclusão usam os nomes de campos das entidades. Aqui o Hibernate compila cada uma contra o
 * modelo (sem banco e sem executar nada), para um nome errado aparecer no teste e não na tela.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"})
class RecordRemovalQueriesTest {

    @TestConfiguration
    static class NoDbTransactions {
        @Bean
        AbstractPlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void everyDeletionQueryIsValidForTheEntityModel() {
        for (RecordRemovalService.Step step : RecordRemovalService.allSteps()) {
            assertNotNull(entityManager.createQuery(step.query()), step.label());
        }
    }
}
