package io.protobj.microserver;

/**
 * Created on 2021/7/12.
 * <p>
 * 用来做线程分发，每个模块一个disruptor
 */
public interface Modularizer{

    SvrType svrType();//属于哪个模块
}
