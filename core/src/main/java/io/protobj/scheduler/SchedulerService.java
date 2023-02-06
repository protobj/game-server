package io.protobj.scheduler;

import io.protobj.IServer;

import java.util.List;

public class SchedulerService {

    private HashedWheelTimer timer;

    public void init(List<Module> moduleList, IServer server) {
        timer = new HashedWheelTimer(server.threadGroup(), 1000, 60, null);






    }


}
