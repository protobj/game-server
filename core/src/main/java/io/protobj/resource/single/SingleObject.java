package io.protobj.resource.single;

import io.protobj.Json;

import java.lang.reflect.Type;

public class SingleObject<T> extends SingleValue {

    private T value;

    public SingleObject(Type type) {
        super( type);
    }

    @Override
    protected synchronized void parse(Json json) {
        this.value = (T) json.decode(source, type);
    }
}
