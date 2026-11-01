package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.controller.support.ColumnField;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import com.quimia.quimiawebcrud.repository.ColumnFilterRepository;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import com.quimia.quimiawebcrud.repository.DisposalRepository;
import com.quimia.quimiawebcrud.repository.HistoryTypeRepository;
import com.quimia.quimiawebcrud.repository.ProductRepository;
import com.quimia.quimiawebcrud.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * As colunas que cada tela oferece em "Atualizar por..." e "Apagar por..." precisam existir na tabela, e as que
 * aceitam novo valor precisam ser aceitas pelo repositório. Sem banco aqui, a conferência é contra o mapeamento
 * do Hibernate, para um nome errado aparecer no teste e não na tela.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"})
class ColumnFieldsMatchRepositoriesTest {

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

    @Autowired
    AdminController adminController;
    @Autowired
    UserController userController;
    @Autowired
    CompanyController companyController;
    @Autowired
    ProductController productController;
    @Autowired
    DisposalController disposalController;
    @Autowired
    HistoryTypeController historyTypeController;

    @Autowired
    AdminRepository admins;
    @Autowired
    UserRepository users;
    @Autowired
    CompanyRepository companies;
    @Autowired
    ProductRepository products;
    @Autowired
    DisposalRepository disposals;
    @Autowired
    HistoryTypeRepository historyTypes;

    private void check(String screen, List<ColumnField> fields, ColumnFilterRepository<?, ?> repository) {
        assertTrue(!fields.isEmpty(), screen + ": sem colunas");
        for (ColumnField field : fields) {
            assertTrue(repository.columns().contains(field.getColumn()),
                    screen + ": a coluna " + field.getColumn() + " não existe na tabela");
            if (field.isUpdatable()) {
                assertTrue(repository.updatableColumns().contains(field.getColumn()),
                        screen + ": a coluna " + field.getColumn() + " não pode ser alterada em lote");
            }
        }
    }

    @Test
    void everyScreenOffersOnlyRealColumnsAndAcceptableUpdates() {
        check("administradores", adminController.getColumnFields(), admins);
        check("usuários", userController.getColumnFields(), users);
        check("empresas", companyController.getColumnFields(), companies);
        check("produtos", productController.getColumnFields(), products);
        check("descarte", disposalController.getColumnFields(), disposals);
        check("tipos de histórico", historyTypeController.getColumnFields(), historyTypes);
    }
}
