package io.protobj.event;

import io.protobj.BeanContainer;
import io.protobj.Module;
import io.protobj.network.MessageListResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Test {

    public void print() {
        System.err.println("Test old");
        new Runnable(){

            @Override
            public void run() {
                System.err.println("Test Runnable old");
            }
        }.run();
    }

    public static class TestModule implements Module {
        public void print() {
            System.err.println("TestModule old");
            new Runnable(){

                @Override
                public void run() {
                    System.err.println("TestModule Runnable old");
                }
            }.run();
        }
    }

    public TestInner testInner = new TestInner();

    public class TestInner{
        public void print() {
            System.err.println("TestInner old");
            new Runnable(){
                @Override
                public void run() {
                    System.err.println("TestInner Runnable old");
                }
            }.run();
        }
    }

    public static void main(String[] args) {
//        DefaultEventBus defaultEventBus = new DefaultEventBus();
//
//        defaultEventBus.register(List.of(new TestModule()), new BeanContainer() {
//            @Override
//            public <T> T getBeanByType(Class<T> clazz) {
//                try {
//                    return clazz.getConstructor().newInstance();
//                } catch (InstantiationException e) {
//                    throw new RuntimeException(e);
//                } catch (IllegalAccessException e) {
//                    throw new RuntimeException(e);
//                } catch (InvocationTargetException e) {
//                    throw new RuntimeException(e);
//                } catch (NoSuchMethodException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//        defaultEventBus.postAsync(MessageListResponse.valueOf(), null, new TestEvent(), Executors.newFixedThreadPool(8));
//        try {
//            TimeUnit.HOURS.sleep(1);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }
}
