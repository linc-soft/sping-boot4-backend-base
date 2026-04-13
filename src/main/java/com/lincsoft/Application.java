package com.lincsoft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point of the application
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@SpringBootApplication
@EnableScheduling
public class Application {
  void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
