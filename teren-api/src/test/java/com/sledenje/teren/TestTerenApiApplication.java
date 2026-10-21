package com.sledenje.teren;

import org.springframework.boot.SpringApplication;

/**
 * Zažene API lokalno (iz IDE) na začasni testni bazi v Dockerju, brez docker-compose.
 */
public class TestTerenApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(TerenApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
