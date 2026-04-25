package com.ecommerce.main.chat;

import com.ecommerce.main.user.Role;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class QueryExecutionService {

    private final EntityManager entityManager;

    // Only SELECT statements allowed
    private static final Pattern ALLOWED = Pattern.compile(
            "^\\s*SELECT\\b.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // Dangerous keywords that must not appear anywhere in the query
    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE",
            "ALTER", "CREATE", "EXEC", "EXECUTE", "--", ";"
    );

    /**
     * Executes a chatbot-generated SQL query with role-based data scope enforcement.
     * Returns raw rows as list of maps (column -> value).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> execute(String sql, Role role, Long scopeId) {
        validateSql(sql);
        String scopedSql = applyScopeFilter(sql, role, scopeId);

        // Convert to list of maps using column metadata via JPA tuple
        var tupleQuery = entityManager.createNativeQuery(scopedSql, jakarta.persistence.Tuple.class);

        @SuppressWarnings("unchecked")
        List<jakarta.persistence.Tuple> tuples = tupleQuery.getResultList();

        return tuples.stream().map(tuple -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            for (var element : tuple.getElements()) {
                row.put(element.getAlias(), tuple.get(element));
            }
            return row;
        }).toList();
    }

    private void validateSql(String sql) {
        if (!ALLOWED.matcher(sql).matches()) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        String upper = sql.toUpperCase();
        for (String blocked : BLOCKED_KEYWORDS) {
            if (upper.contains(blocked)) {
                throw new IllegalArgumentException("Query contains blocked keyword: " + blocked);
            }
        }
    }

    /**
     * Wraps the SQL in a subquery and appends a WHERE clause to enforce data scope.
     * Individual → only their own data (user_id filter)
     * Corporate  → only their store's data (store_id filter)
     * Admin      → no restriction
     */
    private String applyScopeFilter(String sql, Role role, Long scopeId) {
        return switch (role) {
            case INDIVIDUAL -> String.format(
                    "SELECT * FROM (%s) AS _q WHERE _q.user_id = %d", sql, scopeId);
            case CORPORATE -> String.format(
                    "SELECT * FROM (%s) AS _q WHERE _q.store_id = %d", sql, scopeId);
            case ADMIN -> sql;
        };
    }
}
