package com.busconnect.userservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "User Service API",
                version = "1.0.0",
                description = "Microservicio reactivo para gestión de usuarios en BusConnect"
        )
)
@EnableScheduling
@EnableR2dbcAuditing //Para @CreatedDate y @LastModifiedDate en R2DBC
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
