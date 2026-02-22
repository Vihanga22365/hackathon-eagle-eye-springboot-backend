package com.microservices.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Server
 * 
 * This service provides centralized external configuration management for all microservices.
 * It uses a Git repository (can be local or remote) to store configuration files.
 * Services can fetch their configuration from this server based on their application name,
 * profile, and label.
 * 
 * Configuration files should be named as: {application-name}-{profile}.yml
 * Example: user-service-dev.yml, user-service-prod.yml
 * 
 * @author Spring Microservices Team
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
