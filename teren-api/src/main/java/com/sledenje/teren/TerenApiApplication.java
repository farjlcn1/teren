package com.sledenje.teren;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TerenApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TerenApiApplication.class, args);
    }

}
