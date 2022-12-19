package io.protobj.resource.single;

import io.protobj.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

public class SingleLong extends SingleValue {
    private static final Logger log = LoggerFactory.getLogger(SingleLong.class);
    private long value;

    public SingleLong(Type type) {
        super(type);
    }

    @Override
    protected synchronized void parse(Json json) {
        this.value = Long.valueOf(source);
    }

    public long getValue() {
        return value;
    }
}
