package com.example.lifecoach3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LifeCoach3ApplicationTests {

    @Test
    void contextLoads() {
    }

}
