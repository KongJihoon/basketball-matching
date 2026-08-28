package com.example.basketballmatching.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(
        initializers = IntegrationTestInitializer.class
)
public @interface IntegrationTest {
}
