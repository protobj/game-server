package io.protobj.event.service;

import io.protobj.event.EventHolder;
import io.protobj.event.Response;
import io.protobj.event.Subscribe;
import io.protobj.event.TestEvent;

public class TestEventService {

    @Subscribe
    public void execute(Response response, EventHolder holder, TestEvent event) {
        System.err.println(getClass().getName() + " execute : TestEvent " + Thread.currentThread().getName());
        throw new RuntimeException();
    }
}
