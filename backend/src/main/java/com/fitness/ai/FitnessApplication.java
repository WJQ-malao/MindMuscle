package com.fitness.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@MapperScan("com.fitness.ai.mapper")
public class FitnessApplication {

    public static void main(String[] args) {
        // 开启 Reactor 自动上下文传播：ThreadLocal（如登录用户身份）
        // 随流式响应的线程切换自动捕获/恢复，保证 AI 工具调用线程安全
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(FitnessApplication.class, args);
    }
}
