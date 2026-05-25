package com.rodin.SsuBench;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SsuBenchApplication {

    static void main(String[] args) {
        SpringApplication.run(SsuBenchApplication.class, args);
    }

}
