package edu.campus.loop.user;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** 只扫描成员 A 的组件，商品、交易服务不参与独立运行。 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = UserService.class)
public class UserStandaloneApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(UserStandaloneApplication.class);
        app.setDefaultProperties(Map.of("server.address", "127.0.0.1"));
        app.run(args);
    }
}
