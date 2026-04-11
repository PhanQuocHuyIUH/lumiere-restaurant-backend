package iuh.fit.se;

import org.springframework.boot.SpringApplication;

public class TestLumiereRestaurantBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(LumiereRestaurantBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
