package com.shizi.sap_springboot_test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SapSpringbootTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SapSpringbootTestApplication.class, args);
    }

}
