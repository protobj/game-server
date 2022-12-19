package io.protobj.resource;

import io.protobj.Json;

public class ResourceConfig {

    private String resourcePath = "resource/";


    private String reloadPath = "reload/resource/";

    private transient Json json;

    private transient ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public Json getJson() {
        return json;
    }

    public void setJson(Json json) {
        this.json = json;
    }

    public String getReloadPath() {
        return reloadPath;
    }

    public void setReloadPath(String reloadPath) {
        this.reloadPath = reloadPath;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
