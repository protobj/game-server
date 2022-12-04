package io.protobj;

import io.protobj.event.Test;
import io.protobj.hotswap.HotSwapManger;

import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class TestMain {
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, UnknownHostException {
        HotSwapManger hotSwapManger = new HotSwapManger();
        hotSwapManger.start();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                new Test().print();

                Test test = new Test();
                test.testInner.print();

                Test.TestModule testModule = new Test.TestModule();
                testModule.print();
                System.err.println("------------------------");
            }
        }, 0, 2000);
    }
}
