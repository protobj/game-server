package io.protobj;

import io.protobj.scheduler.HashedWheelTimer;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
        Runnable debounce = hashedWheelTimer.debounce(() -> {
            System.err.println("run");
        }, 1000, TimeUnit.MILLISECONDS);
        debounce.run();
        Thread.sleep(100000000);
    }
}