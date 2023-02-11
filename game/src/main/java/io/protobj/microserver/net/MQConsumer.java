package io.protobj.microserver.net;

/**
 * Created on 2021/6/29.
 *
 * @author chen qiang
 */
public abstract class MQConsumer<T> {
    private MQContext<T> mqContext;

    public MQContext<T> getContext() {
        return mqContext;
    }

    public void setMqContext(MQContext<T> mqContext) {
        this.mqContext = mqContext;
    }

    public abstract void close();
}
