package com.cbs.uploads_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {
  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Autowired
  private Environment environment;

  @Bean
  public OpenAPI openAPI() {
    List<Server> servers = new ArrayList<>();

    if (contextPath != null && !contextPath.isEmpty()) {
      servers.add(new Server()
          .url(contextPath)
          .description(String.format(
              "Current %s environment",
              environment.getProperty("spring.profiles.active", "default")
          ))
      );

      servers.add(new Server()
          .url("http://localhost:9003" + (contextPath != null ? contextPath : ""))
          .description("Local environment server")
      );
    }

    return new OpenAPI()
        .info(new Info()
            .title("Uploads Service")
            .version("1.0.0")
            .description("API documentation for Uploads Service")
            .contact(new Contact()
                .name("POR")
                .url("pordev.com")
                .email("porchouayang.vaj@apb.com.la"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0")
            ).license(new License()
                .name("MinioIO")
                .url("http://10.1.11.179:9003/")
            ).license(new License()
                .name("Spring Boot")
                .url("https://spring.io/projects/spring-boot")
            )
        )
        .servers(servers);
  }


}
