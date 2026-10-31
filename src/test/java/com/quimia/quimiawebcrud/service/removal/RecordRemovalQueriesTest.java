package com.quimia.quimiawebcrud.service.removal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os planos de exclusão usam SQL com nomes de tabelas e colunas. Sem banco aqui, o teste confere cada nome
 * contra o mapeamento do Hibernate (as entidades), para um erro de digitação aparecer no teste e não na tela.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"})
class RecordRemovalQueriesTest {

    private static final Set<String> SQL_WORDS = Set.of("select", "from", "where", "in", "or", "and", "id");
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z_]+");

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

    private final Set<String> tables = new HashSet<>();
    private final Set<String> columns = new HashSet<>();

    private void readMapping() {
        SessionFactoryImplementor factory = entityManager.getEntityManagerFactory().unwrap(SessionFactoryImplementor.class);
        factory.getMappingMetamodel().forEachEntityDescriptor(descriptor -> {
            AbstractEntityPersister persister = (AbstractEntityPersister) descriptor;
            tables.add(persister.getTableName());
            columns.addAll(List.of(persister.getIdentifierColumnNames()));
            for (int i = 0; i < persister.getPropertyNames().length; i++) {
                columns.addAll(List.of(persister.getPropertyColumnNames(i)));
            }
        });
    }

    @Test
    void everyTableAndColumnInThePlansExistsInTheEntityMapping() {
        readMapping();
        assertTrue(tables.containsAll(Set.of("usuarios", "historicos", "produtos_usuarios")), "o mapeamento foi lido: " + tables);

        for (RecordRemovalService.Part part : RecordRemovalService.allParts()) {
            assertTrue(tables.contains(part.table()), "tabela desconhecida: " + part.table());
            Matcher words = IDENTIFIER.matcher(part.where());
            while (words.find()) {
                String word = words.group();
                if (SQL_WORDS.contains(word)) {
                    continue;
                }
                assertTrue(tables.contains(word) || columns.contains(word),
                        "nome desconhecido '" + word + "' em: " + part.table() + " where " + part.where());
            }
        }
    }

    @Test
    void theRecordItselfIsAlwaysTheFirstPartOfEachPlan() {
        for (List<RecordRemovalService.Part> plan : List.of(RecordRemovalService.USER_PARTS,
                RecordRemovalService.COMPANY_PARTS, RecordRemovalService.PRODUCT_PARTS,
                RecordRemovalService.HISTORY_TYPE_PARTS, RecordRemovalService.DISPOSAL_PARTS,
                RecordRemovalService.ADMIN_PARTS)) {
            assertTrue(plan.get(0).where().matches("id_[a-z_]+ = :id"), plan.get(0).table());
        }
    }
}
