package io.protobj.resource.single;

import io.protobj.Json;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;

public abstract class SingleValue {

    //资源值
    protected String source;

    //资源类型
    protected Type type;

    public SingleValue(Type type) {
        this.type = type;
    }

    public void setSource(String path, String source, Json json) {
        if (StringUtils.isEmpty(source)) {
            throw new RuntimeException("path:%s source is null ".formatted(path));
        }
        if (this.source.endsWith(source)) {
            return;
        }
        this.source = source;
        parse(json);
    }

    protected abstract void parse(Json json);
}
