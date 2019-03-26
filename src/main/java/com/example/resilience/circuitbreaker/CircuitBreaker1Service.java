package com.example.resilience.circuitbreaker;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class CircuitBreaker1Service {

    @CircuitBreaker(name = "circuitB")
    public String func(String str) {
        if (str == null) {
            throw new RuntimeException();
        }
        return "success!!";
    }

    public String doSomethingThrowException(){
        int a = 0;
        if(a == 0){
            throw new RuntimeException("doSomethingThrowException");
        }
        return "doSomethingThrowException";
    }

    public String doSomethingSlowly(){
        return "doSomethingSlowly";
    }

    public String doSomething(){
        return "doSomething";
    }

//    public String aop(String str) {
//        if (str == null) {
//            throw new RuntimeException();
//        }
//        return "success!!";
//    }
}
