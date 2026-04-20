package com.nap.pos.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Runs lightweight ALTER TABLE migrations on startup for columns that the
 * Hibernate Community SQLite dialect silently skips during ddl-auto=update.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("usuarios", "nombre",   "VARCHAR(60)");
        addColumnIfMissing("usuarios", "apellido",  "VARCHAR(60)");
        addColumnIfMissing("usuarios", "creado_por", "INTEGER REFERENCES usuarios(id)");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        List<Map<String, Object>> columns = jdbc.queryForList("PRAGMA table_info(" + table + ")");
        boolean exists = columns.stream()
                .anyMatch(row -> column.equalsIgnoreCase((String) row.get("name")));
        if (!exists) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("DB migration: added column {}.{}", table, column);
        }
    }
}
