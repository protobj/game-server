package io.protobj;

import io.protobj.hotswap.HotSwapConfig;

public class Config {

    private HotSwapConfig hotSwap;


    public HotSwapConfig getHotSwap() {
        return hotSwap;
    }

    public void setHotSwap(HotSwapConfig hotSwap) {
        this.hotSwap = hotSwap;
    }
}
