package com.english;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class MigrationServiceApplication implements CommandLineRunner {
    private final ConfigurableApplicationContext context;

    @Value("${migration.datasource.url:${DB_URL:jdbc:mysql://localhost:3306/english_learning?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&createDatabaseIfNotExist=true}}")
    private String datasourceUrl;

    @Value("${migration.datasource.username:${DB_USERNAME:root}}")
    private String datasourceUsername;

    @Value("${migration.datasource.password:${DB_PASSWORD:123456}}")
    private String datasourcePassword;

    @Value("${migration.locations:classpath:db/migration}")
    private String locations;

    @Value("${migration.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    public MigrationServiceApplication(ConfigurableApplicationContext context) {
        this.context = context;
    }

    public static void main(String[] args) {
        SpringApplication.run(MigrationServiceApplication.class, args);
    }

    @Override
    public void run(String... args) {
        Flyway flyway = Flyway.configure()
                .dataSource(datasourceUrl, datasourceUsername, datasourcePassword)
                .locations(Arrays.stream(locations.split(","))
                        .map(String::trim)
                        .filter(location -> !location.isEmpty())
                        .toArray(String[]::new))
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion("1")
                .load();

        flyway.migrate();
        SpringApplication.exit(context, () -> 0);
    }
}
