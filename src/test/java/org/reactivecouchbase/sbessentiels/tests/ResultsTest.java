package org.reactivecouchbase.sbessentiels.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = ResultsTest.Application.class)
public class ResultsTest {

    @Test
    public void test() {

    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }
    }
}
