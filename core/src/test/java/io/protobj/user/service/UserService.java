package io.protobj.user.service;

import io.protobj.scheduler.Scheduled;

public class UserService {


    @Scheduled(fixedRate = 1000)
    public void refreshSec() {
        System.err.println("每秒更新 " + System.currentTimeMillis());
    }
}
