package io.protobj.resource.single;

import java.util.Map;

public class SingleFileDesc {

    private String fileName;

    private long lastModified;

    private Map<String, SingleValueDesc> valueDescMap;


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public Map<String, SingleValueDesc> getValueDescMap() {
        return valueDescMap;
    }

    public void setValueDescMap(Map<String, SingleValueDesc> valueDescMap) {
        this.valueDescMap = valueDescMap;
    }
}
