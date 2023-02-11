package io.protobj.mock;

import java.lang.reflect.Method;

/**
 * Created on 2021/5/20.
 *
 * @author chen qiang
 */
public class MethodInvoker<A extends IMockHandler>{
    A mockHandler;
    Method method;

    public MethodInvoker(A mockHandler, Method method) {
        this.mockHandler = mockHandler;
        this.method = method;
    }

    public void invoke(Object... param) {
        try {
            method.invoke(mockHandler, param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
