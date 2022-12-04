package io.protobj.event;

public interface Subscriber<T extends Event> {
    void execute(Response response, EventHolder holder, T event);
}
