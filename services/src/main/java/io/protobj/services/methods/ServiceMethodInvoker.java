package io.protobj.services.methods;

import io.protobj.services.api.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;

public class ServiceMethodInvoker {

    private int cmd;

    private CommunicationMode mode;

    private Method method;

    private Type retType;

    private Type paramType;

    public Publisher<?> invoke(Message request) {
        Publisher<?> result = null;
        Throwable throwable = null;
        try {
            result = (Publisher<?>) method.invoke(request);
            if (result == null) {
                result = Mono.empty();
            }
        } catch (InvocationTargetException ex) {
            throwable = Optional.ofNullable(ex.getCause()).orElse(ex);
        } catch (Throwable ex) {
            throwable = ex;
        }
        return throwable != null ? Mono.error(throwable) : result;
    }


    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Type getRetType() {
        return retType;
    }

    public void setRetType(Type retType) {
        this.retType = retType;
    }

    public Type getParamType() {
        return paramType;
    }

    public void setParamType(Type paramType) {
        this.paramType = paramType;
    }

    public Mono<Message> invokeOne(Message message) {
        return null;
    }

    public Flux<Message> invokeMany(Message message) {
        return null;
    }

    public Publisher<Message> invokeBidirectional(Flux<Message> messages) {
        return null;
    }

    public Mono<Void> invokeNull(Message message) {
        return null;
    }
}

