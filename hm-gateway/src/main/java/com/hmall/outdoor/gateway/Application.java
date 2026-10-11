package com.hmall.outdoor.gateway;

import com.hmall.outdoor.common.Tokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  Tokens tokens(
    @Value("${outdoor.token-secret:shanye-local-demo-token-key-change-for-deployment}") String key
  ) {
    return new Tokens(key);
  }
}
