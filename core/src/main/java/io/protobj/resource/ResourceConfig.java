package io.protobj.resource;

import io.protobj.Json;

public class ResourceConfig {

    private String resourcePath = "classpath";

    private transient Json json;

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public Json getJson() {
        return json;
    }
}
