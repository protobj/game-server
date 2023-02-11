package org.protobj.mock.plan;

import com.guangyu.cd003.projects.gs.module.chat.msg.RqstChat;
import com.guangyu.cd003.projects.mock.config.GmCreConfig;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.guangyu.cd003.projects.common.cons.Commands.CHAT_SEND_MSG_CONST;

/**
 * Created on 2021/5/21.
 *
 * @author chen qiang
 */
public class GMPlan extends Plan {

    private static final Logger logger = LoggerFactory.getLogger(GMPlan.class);

    public static AtomicInteger completeCount = new AtomicInteger();

    public GMPlan() {
        super();
    }

    @Override
    protected Observable<Integer> execute0(MockConnect connect) {
        GmCreConfig config = (GmCreConfig) connect.mockContext.config;
        if (config.getCreLv() == 0 || connect.ROLE_DATA.respRoleInfo.roleInfo.lv > 1) {
            return Observable.empty();
        }
        return Observable.fromCompletionStage(sendGmCre(connect)).doOnNext(t -> {
            logger.info("已完成 {} 个", completeCount.incrementAndGet());
        });
    }

    private static CompletableFuture<Integer> sendGmCre(MockConnect connect) {
        GmCreConfig config = (GmCreConfig) connect.mockContext.config;
        RqstChat rqst = new RqstChat();
        rqst.channel = 10;
        rqst.msg = "/cmd gmcre " + config.getCreLv();
        return connect.send(CHAT_SEND_MSG_CONST, rqst);
    }
}
