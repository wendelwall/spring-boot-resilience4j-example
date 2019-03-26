package com.example.resilience.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/circuit1")
public class CircuitBreaker1Controller {

    @Autowired
    CircuitBreaker1Service service;


    @GetMapping("/func")
    public String func(@RequestParam(required = false) String str) {
        return Try.ofSupplier(() -> service.func(str))
                .recover(CircuitBreakerOpenException.class, "服务器不可用，断路器已打开！")
                .recover(RuntimeException.class, "服务器繁忙！").get();
    }

//    @GetMapping("/aop")
//    public String aop(@RequestParam(required = false) String str) {
//        return Try.ofSupplier(() -> service.aop(str)).recover(CircuitBreakerOpenException.class, "Circuit is Open!!")
//                .recover(RuntimeException.class, "fallback!!").get();
//    }
}
