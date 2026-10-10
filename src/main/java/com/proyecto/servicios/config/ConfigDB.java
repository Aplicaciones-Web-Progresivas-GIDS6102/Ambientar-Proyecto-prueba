package com.proyecto.servicios.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.proyecto.servicios.repositorys"
        },
        transactionManagerRef = "sfTransactionManager",
        entityManagerFactoryRef = "sfEntityManagerFactory"
)
public class ConfigDB {
    @Autowired
    private Environment env;

    @Bean(name="sfDatasource")
    public DataSource sfDatasource(){
        HikariConfig config=new HikariConfig();
        try{
            String url = env.getProperty("SPRING_DATASOURCE_URL");
            if (url == null || url.trim().isEmpty()) {
                url = env.getProperty("spring.datasource.url");
            }
            if (url != null && url.startsWith("postgresql://")) {
                url = "jdbc:" + url;
            }
            
            String username = env.getProperty("SPRING_DATASOURCE_USERNAME");
            if (username == null || username.trim().isEmpty()) {
                username = env.getProperty("spring.datasource.username");
            }
            
            String password = env.getProperty("SPRING_DATASOURCE_PASSWORD");
            if (password == null || password.trim().isEmpty()) {
                password = env.getProperty("spring.datasource.password");
            }

            log.info("Conectando a base de datos JDBC URL: {}", url);
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setMaxLifetime(1800000);
            config.setConnectionTimeout(10000);
            config.setValidationTimeout(5000);
            config.setMinimumIdle(2);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("sfDatasource");

            return new HikariDataSource(config);
        }catch (Exception e){
            log.error("Ha ocurrido un error en la conexion a base de datos, a causa de:",e);
            throw new IllegalStateException("Falló la inicialización de la base de datos sfDatasource: " + e.getMessage(), e);
        }
    }

    @Bean(name="sfEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean sfEntityManagerFactory(){
        LocalContainerEntityManagerFactoryBean em= new LocalContainerEntityManagerFactoryBean();
        try{
          em.setDataSource(sfDatasource());
          em.setPackagesToScan(
                  "com.proyecto.servicios.entity",
                  "com.proyecto.servicios.entity.sf",
                  "com.proyecto.servicios.entity.gestopago"
          );
          em.setPersistenceUnitName("sfDatasource");
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);
          Map<String, Object> properties=new HashMap<>();
          properties.put("hibernate.hbm2ddl.auto", "none");
          properties.put("hibernate.show-sql", false);
          properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
          properties.put("jakarta.persistence.query.timeout", 600000);

          em.setJpaPropertyMap(properties);

          return em;
        } catch (Exception e) {
            log.error("Ha ocurrido un error en la conexion a base de datos, a causa de:",e);
            throw new IllegalStateException("Falló la creación de sfEntityManagerFactory: " + e.getMessage(), e);
        }
    }
 @Bean(name="sfTransactionManager")
 public PlatformTransactionManager sfTransactionManager(@Qualifier("sfEntityManagerFactory") EntityManagerFactory sfEntityManagerFactory){
        return new JpaTransactionManager(sfEntityManagerFactory);

 }

}
