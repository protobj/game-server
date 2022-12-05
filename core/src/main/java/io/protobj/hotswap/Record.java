package io.protobj.hotswap;

import java.util.Map;

public class Record {

    String filePath;
    long lastModified;
    long updateTime;
    String fileContent;
    Map<String, ClassRecord> classes;


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String fileName) {
        this.filePath = fileName;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public Map<String, ClassRecord> getClasses() {
        return classes;
    }

    public void setClasses(Map<String, ClassRecord> classes) {
        this.classes = classes;
    }

    public static record ClassRecord(String clazzName, byte[] compiledBytes) {
    }
}




