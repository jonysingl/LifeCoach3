package com.example.lifecoach3;

import org.springframework.boot.SpringApplication;

public class TestLifeCoach3Application {

    public static void main(String[] args) {
        SpringApplication.from(LifeCoach3Application::main).with(TestcontainersConfiguration.class).run(args);
    }

}
