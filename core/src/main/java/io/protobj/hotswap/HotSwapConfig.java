package io.protobj.hotswap;

public class HotSwapConfig {

    private int httpPort;

    private String swapDir;

    private String addDir;

    public String getSwapDir() {
        return swapDir;
    }

    public void setSwapDir(String swapDir) {
        this.swapDir = swapDir;
    }

    public String getAddDir() {
        return addDir;
    }

    public void setAddDir(String addDir) {
        this.addDir = addDir;
    }

    public HotSwapConfig(String swapDir, String addDir) {
        this.swapDir = swapDir;
        this.addDir = addDir;
    }

    public HotSwapConfig(String swapDir, String addDir,int httpPort) {
        this.swapDir = swapDir;
        this.addDir = addDir;
        this.httpPort = httpPort;
    }

    public HotSwapConfig() {
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }
}
