package io.protobj.event.service;

import io.protobj.event.EventHolder;
import io.protobj.event.Response;
import io.protobj.event.Subscriber;
import io.protobj.event.TestEvent;

public class TestEventSubscriber implements Subscriber<TestEvent> {
    @Override
    public void execute(Response response, EventHolder holder, TestEvent event) {
        System.err.println(getClass().getName() + " execute : TestEvent " + Thread.currentThread().getName());
        throw new RuntimeException();
    }
}
