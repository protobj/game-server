package io.protobj.mock.plan;

import io.protobj.mock.net.MockConnect;
import io.reactivex.rxjava3.core.Observable;

public class LoginLogoutPlan extends Plan {
    @Override
    protected Observable<Integer> execute0(MockConnect connect) {
//TODO        return RoleController.loadOrCreateRole(connect)
//                .concatMap(t -> Observable.fromCompletionStage(connect.tryClose()))
//                .concatWith(Observable.fromRunnable(() -> connect.mockContext.connect(connect)));
        return Observable.empty();
    }


    @Override
    public long getDelay() {
        return 100;
    }

}
