package org.protobj.mock.module.role;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.role.msg.CodeRole;
import com.guangyu.cd003.projects.gs.module.role.msg.RqstCreRole;
import com.guangyu.cd003.projects.gs.module.role.msg.RqstLoadRole;
import com.guangyu.cd003.projects.mock.net.CodeException;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.net.RqstFuture;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    public static Observable<Integer> loadOrCreateRole(MockConnect connect) {
        return Observable
                .fromCompletionStage(load(connect))
                .onErrorResumeNext((err) -> {
                    if (CodeException.getCodeInEx(err) == CodeRole.CANNT_FIND_ROLE.getCode()) {
                        return Observable.fromCompletionStage(createRole(connect));
                    }
                    return Observable.error(err);
                });
    }


    public static CompletableFuture<Integer> load(MockConnect connect) {
        RqstLoadRole rqstLoadRole = new RqstLoadRole();
        rqstLoadRole.sid = connect.getServerId().split("_")[1];
        RqstFuture rqstFuture = connect.send(Commands.ROLE_LOAD_CONST, rqstLoadRole);
        return rqstFuture.thenApply((t) -> {
            onLoginSuccess(connect);
            return t;
        });
    }

    private static void onLoginSuccess(MockConnect connect) {
        logger.info("login {}", connect.getUid());
        connect.mockContext.connectMap.put(connect.getUid(), connect);
    }

    public static CompletableFuture<Integer> createRole(MockConnect connect) {
        //创建角色
        RqstCreRole rqstLoadRole = new RqstCreRole();
        rqstLoadRole.sid = connect.getServerId().split("_")[1];
        rqstLoadRole.country = 1;
        rqstLoadRole.name = connect.getUid();
        rqstLoadRole.camp = 1;
        RqstFuture future = connect.send(Commands.ROLE_CRE_CONST, rqstLoadRole);
        return future.thenApply(t -> {
            onLoginSuccess(connect);
            return t;
        });

    }

    public static CompletableFuture<Integer> logout(MockConnect connect) {
        return connect.tryClose();
    }
}
