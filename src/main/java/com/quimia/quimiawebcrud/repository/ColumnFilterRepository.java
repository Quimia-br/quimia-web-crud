package com.quimia.quimiawebcrud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base de todos os repositórios: além do CRUD do JpaRepository, procura, altera e apaga as linhas da tabela
 * filtrando por uma coluna. Os nomes são os das colunas no banco (ex.: "nome"); o valor pode vir como texto
 * (é convertido para o tipo da coluna) e, vazio ou nulo, significa "coluna sem valor".
 */
@NoRepositoryBean
public interface ColumnFilterRepository<T, ID> extends JpaRepository<T, ID> {

    /** Colunas da tabela da entidade: as que podem ser usadas como filtro. */
    Set<String> columns();

    /**
     * Colunas que podem ser alteradas em lote: ficam de fora o id, as colunas únicas (a mesma alteração em várias
     * linhas as repetiria), as que a entidade não deixa mudar e a senha.
     */
    Set<String> updatableColumns();

    /** Registros em que {@code column} é igual a {@code value}. */
    List<T> findByColumn(String column, Object value);

    /**
     * Apaga direto no banco as linhas em que {@code column} é igual a {@code value}, sem apagar o que depende delas.
     * Devolve quantas linhas foram apagadas.
     */
    int deleteByColumn(String column, Object value);

    /**
     * Altera, nas linhas em que {@code filterColumn} é igual a {@code filterValue}, as colunas de {@code changes}
     * (nome da coluna e novo valor; valor vazio ou nulo limpa a coluna). Devolve quantas linhas foram alteradas.
     */
    int updateByColumn(String filterColumn, Object filterValue, Map<String, ?> changes);
}
