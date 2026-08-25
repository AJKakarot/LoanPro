package com.loanpro;

import com.loanpro.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@ConfigurationPropertiesScan
public class LoanProcessingApplication {

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(LoanProcessingApplication.class, args);
    }
}
