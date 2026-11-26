package org.veronicadavichi.testserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.bind.annotation.*;

/**
 * 简单 Spring Boot 测试服务器
 * 用于集成测试模拟后端服务
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@RestController
@EnableAspectJAutoProxy
public class RestServer {

    /**
     * 测试接口 - 返回固定结果
     */
    @GetMapping("/test/{id}")
    public ApiResponse test(@PathVariable String id) {

        System.out.println(id);
        return new ApiResponse(true);
    }

    public static class Request {
        RequestA paramA;
        String param2;
    }

    public static class RequestA {
        String param1;
    }

    @PostMapping
    public ApiResponse test1(@RequestBody Request request)  {
        return new ApiResponse(true);
    }

    public static void main(String[] args) {
        // 默认使用 testserver profile，除非命令行指定了其他 profile
        java.util.Set<String> profiles = new java.util.HashSet<>();
        profiles.add("testserver");
        SpringApplication app = new SpringApplication(RestServer.class);
        app.setAdditionalProfiles(profiles.toArray(new String[0]));
        app.run(args);
    }

    /**
         * 统一响应格式
         */
        public record ApiResponse(boolean data) {

    }
}
