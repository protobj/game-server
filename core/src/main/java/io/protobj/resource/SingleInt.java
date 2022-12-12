package io.protobj.resource;

import io.protobj.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

public class SingleInt extends SingleValue {
    private static final Logger log = LoggerFactory.getLogger(SingleInt.class);
    private volatile int value;


    public SingleInt(Type type) {
        super(type);
    }

    @Override
    protected synchronized void parse(Json json) {
        this.value = Integer.valueOf(source);
    }

    public int getValue() {
        return value;
    }

    public static SingleInt valueOf(int value) {
        SingleInt singleInt = new SingleInt(int.class);
        singleInt.setSource("", String.valueOf(value), null);
        return singleInt;
    }
}
