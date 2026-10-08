package com.smartcommute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application entry point.
 *
 * <p>The backend is a modular monolith (see docs/02-architecture.md). Modules are separated by
 * package under {@code com.smartcommute} with a strict dependency direction: web -> application ->
 * domain, with providers and persistence sitting behind interfaces (ports).</p>
 */
@SpringBootApplication
@EnableAsync
public class SmartCommuteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCommuteApplication.class, args);
    }
}
