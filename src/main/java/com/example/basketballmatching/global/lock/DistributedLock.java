package com.example.basketballmatching.global.lock;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    String key();

    long waitTimeMs() default 3000;

    long leaseTimeMs() default 10000;
}
