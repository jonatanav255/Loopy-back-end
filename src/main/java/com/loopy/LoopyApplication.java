package com.loopy;

// Dependencies: @SpringBootApplication, @EnableJpaAuditing, SpringApplication.run — see DEPENDENCY_GUIDE.md
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LoopyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoopyApplication.class, args);
    }
}
