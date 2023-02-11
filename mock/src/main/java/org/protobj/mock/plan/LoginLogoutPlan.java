package org.protobj.mock.plan;

import com.guangyu.cd003.projects.mock.module.role.RoleController;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import io.reactivex.rxjava3.core.Observable;

public class LoginLogoutPlan extends Plan {
    @Override
    protected Observable<Integer> execute0(MockConnect connect) {
        return RoleController.loadOrCreateRole(connect)
                .concatMap(t -> Observable.fromCompletionStage(connect.tryClose()))
                .concatWith(Observable.fromRunnable(() -> connect.mockContext.connect(connect)));
    }

    @Override
    public long getDelay() {
        return 100;
    }

}
