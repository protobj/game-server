package io.protobj.microserver;

/**
 * Created on 2021/7/12.
 * <p>
 * 用来做线程分发，每个模块一个disruptor
 */
public interface Modularizer{

    ServerType ServerType();//属于哪个模块
}
