package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.exception.InvalidDataException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os nomes de coluna do deleteByColumn e do updateByColumn entram direto no SQL: sem banco aqui, o teste confere que
 * só passa coluna mapeada na entidade (e, para alterar, uma que faz sentido alterar em lote), e que qualquer outra
 * coisa, inclusive um valor que não cabe no tipo da coluna, para antes de chegar ao banco.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"})
class ColumnFilterRepositoryTest {

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
    ProductRepository productRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    DisposalRepository disposalRepository;
    @Autowired
    HistoryRepository historyRepository;

    @Test
    void unknownColumnIsRejected() {
        assertThrows(InvalidDataException.class, () -> productRepository.deleteByColumn("nao_existe", "x"));
        assertThrows(InvalidDataException.class, () -> productRepository.deleteByColumn(null, "x"));
    }

    @Test
    void sqlInTheColumnNameIsRejected() {
        assertThrows(InvalidDataException.class, () -> productRepository.deleteByColumn("nome = nome or 1", "x"));
    }

    @Test
    void columnOfAnotherTableIsRejected() {
        // id_produto existe em historicos_produtos, não em historicos
        assertThrows(InvalidDataException.class, () -> historyRepository.deleteByColumn("id_produto", 1));
    }

    @Test
    void idUniqueNonUpdatableAndPasswordColumnsAreNotUpdatable() {
        assertTrue(userRepository.updatableColumns().containsAll(Set.of("nome", "status", "data_nascimento", "url_foto")));
        for (String column : new String[]{"id_usuario", "email", "data_cadastro", "senha_hash"}) {
            assertFalse(userRepository.updatableColumns().contains(column), column + " não deve ser alterável");
            assertTrue(userRepository.columns().contains(column), column + " continua servindo de filtro");
        }
        assertTrue(productRepository.updatableColumns().containsAll(Set.of("marca", "id_usuario_empresa")));
        assertFalse(productRepository.updatableColumns().contains("cas_number"));
        assertFalse(disposalRepository.updatableColumns().contains("id_produto"));
    }

    @Test
    void updateRejectsColumnsThatCannotBeUpdated() {
        assertThrows(InvalidDataException.class, () -> userRepository.updateByColumn("nome", "Ana", Map.of("email", "x@y.com")));
        assertThrows(InvalidDataException.class, () -> userRepository.updateByColumn("nome", "Ana", Map.of("senha_hash", "x")));
        assertThrows(InvalidDataException.class, () -> userRepository.updateByColumn("nome", "Ana", Map.of("id_usuario", 1)));
        assertThrows(InvalidDataException.class, () -> userRepository.updateByColumn("nome", "Ana", Map.of("status = 'ATIVO', nome", "x")));
    }

    @Test
    void updateRejectsAnUnknownFilterColumnOrNothingToChange() {
        assertThrows(InvalidDataException.class, () -> userRepository.updateByColumn("nao_existe", "x", Map.of("status", "ATIVO")));
        assertThrows(InvalidDataException.class, () -> userRepository.updateByColumn("nome", "Ana", Map.of()));
        assertThrows(InvalidDataException.class, () -> userRepository.updateByColumn("nome", "Ana", null));
    }

    @Test
    void valuesThatDoNotFitTheColumnTypeAreRejected() {
        assertThrows(InvalidDataException.class, () -> userRepository.updateByColumn("nome", "Ana", Map.of("status", "NAO_EXISTE")));
        assertThrows(InvalidDataException.class, () -> productRepository.updateByColumn("nome", "Acetona", Map.of("id_usuario_empresa", "abc")));
        assertThrows(InvalidDataException.class, () -> userRepository.updateByColumn("nome", "Ana", Map.of("data_nascimento", "ontem")));
        assertThrows(InvalidDataException.class, () -> productRepository.findByColumn("id_usuario_empresa", "abc"));
    }
}
