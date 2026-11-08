package ru.migrenenet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MigrenenetApplication {
    public static void main(String[] args) {
        SpringApplication.run(MigrenenetApplication.class, args);
    }
}
