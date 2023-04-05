package io.protobj.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

public class ReactorTest {
    public static void main(String[] args) {
        Flux.range(1, 10)
                .map(integer -> {
                    System.out.println(Thread.currentThread().getName());
                    return integer;
                })
//                .publishOn(Schedulers.parallel())
//                .subscribeOn(Schedulers.single())
                .subscribe(integer -> {
                    System.err.println(Thread.currentThread().getName());
                })
        ;
        LockSupport.park();
    }
}
