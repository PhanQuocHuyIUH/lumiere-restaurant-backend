package iuh.fit.se;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import java.util.TimeZone;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static {
        // Ensure PostgreSQL JDBC startup parameter TimeZone is valid
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
                .withEnv("TZ", "UTC")
                .withEnv("PGTZ", "UTC");
    }

}
