package com.proyecto.servicios.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] locations;

    @Value("${spring.flyway.table:flyway_schema_history}")
    private String historyTable;

    @Value("${spring.flyway.schemas:public}")
    private String schema;

    @Bean(name = "flyway")
    public Flyway flyway(@Qualifier("sfDatasource") DataSource dataSource) {
        log.info("Iniciando migraciones Flyway en schema '{}'", schema);
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .table(historyTable)
                .schemas(schema)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        
        // Repara automáticamente el historial de checksums si un archivo de migración fue editado en desarrollo
        log.info("Ejecutando flyway.repair() para actualizar checksums y sincronizar historial de migraciones");
        flyway.repair();
        
        log.info("Ejecutando flyway.migrate()");
        flyway.migrate();
        
        return flyway;
    }
}
