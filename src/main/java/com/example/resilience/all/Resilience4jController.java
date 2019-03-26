package com.example.resilience.all;

import com.example.resilience.circuitbreaker.CircuitBreaker1Service;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * @author ：sunrise
 * @description ：
 * @copyright ：	Copyright 2019 yowits Corporation. All rights reserved.
 * @create ：2019/3/26 22:19
 */
@RestController
public class Resilience4jController {
    @Autowired
    CircuitBreaker1Service service;

    /**
     * CircuitBreaker主要是实现针对接口异常的断路统计以及断路处理
     */
    @GetMapping("/func")
    public void testCircuitBreaker(@RequestParam(required = false) String str) {
        // Create a CircuitBreaker (use default configuration)
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig
                .custom()
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker
                .of("backendName", circuitBreakerConfig);
        String result = circuitBreaker.executeSupplier(() -> service.func(str));
        System.out.println(result);
    }

    /**
     * Timelimiter主要是实现超时的控制
     */
    @GetMapping("/func")
    public void testTimelimiter(@RequestParam(required = false) String str) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(600))
                .cancelRunningFuture(true)
                .build();
        TimeLimiter timeLimiter = TimeLimiter.of(config);

        Supplier<Future<String>> futureSupplier = () -> {
            return executorService.submit(service::doSomethingThrowException);
        };
        Callable<String> restrictedCall = TimeLimiter.decorateFutureSupplier(timeLimiter, futureSupplier);
        Try.of(restrictedCall::call)
                .onFailure(throwable -> System.out.println("We might have timed out or the circuit breaker has opened."));
    }

    /**
     * Bulkhead目前来看是用来控制并行(parallel)调用的次数
     */
    @GetMapping("/func")
    public void testBulkhead(@RequestParam(required = false) String str) {
        Bulkhead bulkhead = Bulkhead.of("test", BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .build());
        Supplier<String> decoratedSupplier = Bulkhead.decorateSupplier(bulkhead, service::doSomethingSlowly);
        IntStream.rangeClosed(1, 2)
                .parallel()
                .forEach(i -> {
                    String result = Try.ofSupplier(decoratedSupplier)
                            .recover(throwable -> "Hello from Recovery").get();
                    System.out.println(result);
                });

    }

    /**
     * RateLimiter 用来做流控
     */
    @GetMapping("/func")
    public void testRateLimiter(@RequestParam(required = false) String str) {
        // Create a custom RateLimiter configuration
        RateLimiterConfig config = RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(100))
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .build();
        // Create a RateLimiter
        RateLimiter rateLimiter = RateLimiter.of("backendName", config);

        // Decorate your call to BackendService.doSomething()
        Supplier<String> restrictedSupplier = RateLimiter
                .decorateSupplier(rateLimiter, service::doSomething);

        IntStream.rangeClosed(1, 5)
                .parallel()
                .forEach(i -> {
                    Try<String> aTry = Try.ofSupplier(restrictedSupplier);
                    System.out.println(aTry.isSuccess());
                });
    }

    /**
     * fallback基本上是高可用操作的标配
     */
    @GetMapping("/func")
    public void testFallback(@RequestParam(required = false) String str) {
        // Execute the decorated supplier and recover from any exception
        String result = Try.ofSupplier(() -> service.doSomethingThrowException())
                .recover(throwable -> "Hello from Recovery").get();
        System.out.println(result);
    }

    public void testCircuitBreakerAndFallback() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        Supplier<String> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, service::doSomethingThrowException);
        String result = Try.ofSupplier(decoratedSupplier)
                .recover(throwable -> "Hello from Recovery").get();
        System.out.println(result);
    }

    /**
     * retry用来控制重试
     */
    @GetMapping("/func")
    public void testRetry(@RequestParam(required = false) String str) {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        // Create a Retry with at most 3 retries and a fixed time interval between retries of 500ms
        Retry retry = Retry.ofDefaults("backendName");

        // Decorate your call to BackendService.doSomething() with a CircuitBreaker
        Supplier<String> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, service::doSomething);

        // Decorate your call with automatic retry
        decoratedSupplier = Retry
                .decorateSupplier(retry, decoratedSupplier);

        // Execute the decorated supplier and recover from any exception
        String result = Try.ofSupplier(decoratedSupplier)
                .recover(throwable -> "Hello from Recovery").get();
        System.out.println(result);
    }
}
