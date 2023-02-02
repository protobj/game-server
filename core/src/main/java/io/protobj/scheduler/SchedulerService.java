package io.protobj.scheduler;

import io.protobj.IServer;

import java.util.List;

public class SchedulerService {

    private HashedWheelTimer timer;

    public void init(List<Module> moduleList, IServer server) {
        timer = new HashedWheelTimer(server.threadGroup(), HashedWheelTimer.DEFAULT_RESOLUTION, HashedWheelTimer.DEFAULT_WHEEL_SIZE, new WaitStrategy.SleepWait());
    }


}
