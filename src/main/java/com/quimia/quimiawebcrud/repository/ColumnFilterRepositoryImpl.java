package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.exception.InvalidDataException;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Query;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Implementação de {@link ColumnFilterRepository} usada por todos os repositórios (ver JpaConfig).
 * Os nomes de coluna entram direto no SQL, então só passam se forem colunas mapeadas na própria entidade;
 * os valores sempre vão como parâmetros.
 */
public class ColumnFilterRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> implements ColumnFilterRepository<T, ID> {

    /** A senha só muda pelo cadastro, que a criptografa. */
    private static final Set<String> NEVER_UPDATED = Set.of("senha_hash");

    private final EntityManager entityManager;
    private final Class<T> domainClass;

    public ColumnFilterRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.domainClass = entityInformation.getJavaType();
    }

    @Override
    public Set<String> columns() {
        return columnsOf(persister());
    }

    @Override
    public Set<String> updatableColumns() {
        AbstractEntityPersister persister = persister();
        Set<String> unique = uniqueColumns();
        Set<String> columns = new HashSet<>();
        boolean[] updatable = persister.getPropertyUpdateability();
        for (int i = 0; i < updatable.length; i++) {
            if (updatable[i]) {
                for (String name : persister.getPropertyColumnNames(i)) {
                    String column = name.toLowerCase(Locale.ROOT);
                    if (!unique.contains(column) && !NEVER_UPDATED.contains(column)) {
                        columns.add(column);
                    }
                }
            }
        }
        return columns;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<T> findByColumn(String column, Object value) {
        AbstractEntityPersister persister = persister();
        String name = mappedColumn(persister, column);
        Object typed = convert(persister, name, value);
        String sql = "select * from " + persister.getTableName() + " where " + name
                + (typed == null ? " is null" : " = :value");
        Query query = entityManager.createNativeQuery(sql, domainClass);
        if (typed != null) {
            query.setParameter("value", typed);
        }
        return query.getResultList();
    }

    @Override
    @Transactional
    public int deleteByColumn(String column, Object value) {
        AbstractEntityPersister persister = persister();
        String name = mappedColumn(persister, column);
        Object typed = convert(persister, name, value);
        String sql = "delete from " + persister.getTableName() + " where " + name
                + (typed == null ? " is null" : " = :value");
        Query query = entityManager.createNativeQuery(sql);
        if (typed != null) {
            query.setParameter("value", typed);
        }
        return run(query);
    }

    @Override
    @Transactional
    public int updateByColumn(String filterColumn, Object filterValue, Map<String, ?> changes) {
        AbstractEntityPersister persister = persister();
        String name = mappedColumn(persister, filterColumn);
        Object typedFilter = convert(persister, name, filterValue);
        if (changes == null || changes.isEmpty()) {
            throw new InvalidDataException("Informe o que deve ser alterado.");
        }

        Set<String> updatable = updatableColumns();
        List<String> assignments = new ArrayList<>();
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (Map.Entry<String, ?> change : changes.entrySet()) {
            String column = normalize(change.getKey());
            if (column == null || !updatable.contains(column)) {
                throw new InvalidDataException("A coluna " + change.getKey() + " não pode ser alterada em lote.");
            }
            Object typed = convert(persister, column, change.getValue());
            if (typed == null) {
                assignments.add(column + " = null");
            } else {
                String parameter = "c" + parameters.size();
                assignments.add(column + " = :" + parameter);
                parameters.put(parameter, typed);
            }
        }

        String sql = "update " + persister.getTableName() + " set " + String.join(", ", assignments)
                + " where " + name + (typedFilter == null ? " is null" : " = :filter");
        Query query = entityManager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        if (typedFilter != null) {
            query.setParameter("filter", typedFilter);
        }
        return run(query);
    }

    /** Executa a alteração direto no banco; o que estava carregado na sessão pode ficar desatualizado. */
    private int run(Query query) {
        int rows = query.executeUpdate();
        entityManager.clear();
        return rows;
    }

    private AbstractEntityPersister persister() {
        return (AbstractEntityPersister) entityManager.getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class)
                .getMappingMetamodel()
                .getEntityDescriptor(domainClass);
    }

    private static String mappedColumn(AbstractEntityPersister persister, String column) {
        String name = normalize(column);
        if (name == null || !columnsOf(persister).contains(name)) {
            throw new InvalidDataException("Coluna inválida para a tabela " + persister.getTableName() + ": " + column);
        }
        return name;
    }

    private static String normalize(String column) {
        return column == null ? null : column.trim().toLowerCase(Locale.ROOT);
    }

    /** Colunas da tabela da entidade: as do id e as dos campos (listas como @ManyToMany não têm coluna aqui). */
    private static Set<String> columnsOf(AbstractEntityPersister persister) {
        Set<String> columns = new HashSet<>();
        addAll(columns, persister.getIdentifierColumnNames());
        for (int i = 0; i < persister.getPropertyNames().length; i++) {
            addAll(columns, persister.getPropertyColumnNames(i));
        }
        return columns;
    }

    private static void addAll(Set<String> columns, String[] names) {
        for (String name : names) {
            columns.add(name.toLowerCase(Locale.ROOT));
        }
    }

    /** Colunas marcadas como únicas na entidade (@Column ou @JoinColumn com unique = true). */
    private Set<String> uniqueColumns() {
        Set<String> unique = new HashSet<>();
        for (Class<?> type = domainClass; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Column column = field.getAnnotation(Column.class);
                if (column != null && column.unique()) {
                    unique.add(column.name().toLowerCase(Locale.ROOT));
                }
                JoinColumn join = field.getAnnotation(JoinColumn.class);
                if (join != null && join.unique()) {
                    unique.add(join.name().toLowerCase(Locale.ROOT));
                }
            }
        }
        return unique;
    }

    /**
     * Valor no tipo da coluna: o que chega da tela é texto. Vazio vira nulo. Textos não são alterados;
     * números, datas, sim/não e valores de lista (enum) são lidos conforme a coluna.
     */
    private static Object convert(AbstractEntityPersister persister, String column, Object value) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            return null;
        }
        if (value instanceof Enum<?> constant) {
            return constant.name();
        }
        if (!(value instanceof String text)) {
            return value;
        }
        Class<?> type = typeOf(persister, column);
        try {
            if (type == Integer.class || type == int.class) {
                return Integer.valueOf(text.trim());
            }
            if (type == Long.class || type == long.class) {
                return Long.valueOf(text.trim());
            }
            if (type == BigDecimal.class) {
                return new BigDecimal(text.trim());
            }
            if (type == Double.class || type == double.class) {
                return Double.valueOf(text.trim());
            }
            if (type == Boolean.class || type == boolean.class) {
                return parseBoolean(text.trim());
            }
            if (type == LocalDate.class) {
                return LocalDate.parse(text.trim());
            }
            if (type == LocalDateTime.class) {
                return LocalDateTime.parse(text.trim());
            }
            if (type.isEnum()) {
                return parseEnum(type, text.trim());
            }
        } catch (RuntimeException e) {
            throw new InvalidDataException("O valor \"" + text + "\" não é válido para a coluna " + column + ".");
        }
        return text;
    }

    private static Boolean parseBoolean(String text) {
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new IllegalArgumentException(text);
    }

    /** As listas (enum) são gravadas pelo nome da constante. */
    private static String parseEnum(Class<?> type, String text) {
        for (Object constant : type.getEnumConstants()) {
            String name = ((Enum<?>) constant).name();
            if (name.equalsIgnoreCase(text)) {
                return name;
            }
        }
        throw new IllegalArgumentException(text);
    }

    /** Tipo Java da coluna; colunas de chave estrangeira guardam o id do registro relacionado (inteiro). */
    private static Class<?> typeOf(AbstractEntityPersister persister, String column) {
        if (List.of(persister.getIdentifierColumnNames()).contains(column)) {
            return persister.getIdentifierType().getReturnedClass();
        }
        Type[] types = persister.getPropertyTypes();
        for (int i = 0; i < types.length; i++) {
            for (String name : persister.getPropertyColumnNames(i)) {
                if (name.equalsIgnoreCase(column)) {
                    return types[i] instanceof EntityType ? Integer.class : types[i].getReturnedClass();
                }
            }
        }
        return String.class;
    }
}
