package org.protobj.mock.module.mail;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.mail.msg.RqstMailIds;
import com.guangyu.cd003.projects.mock.net.MockConnect;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public enum MailState {

    Do {
        @Override
        public void execute(MockConnect mockConnect) {
            MailData mail_data = mockConnect.MAIL_DATA;
            mail_data.mailState = Rqst;
            Set<Integer> mailTypes = mail_data.getRectMail();
            CompletableFuture<Integer> future = null;
            if (!mailTypes.isEmpty()) {
                RqstMailIds sendBytes = new RqstMailIds();
                Iterator<Integer> iterator = mailTypes.iterator();
                sendBytes.mailType = iterator.next();
                iterator.remove();
                future = mockConnect.send(Commands.MAIL_PICK_BATCH_CONST, sendBytes);
            }
            Set<Integer> delMail = mail_data.getDelMail();
            if (!delMail.isEmpty()) {
                RqstMailIds sendBytes = new RqstMailIds();
                Iterator<Integer> iterator = delMail.iterator();
                sendBytes.mailType = iterator.next();
                iterator.remove();
                if (future == null) {
                    future = mockConnect.send(Commands.MAIL_DEL_BATCH_CONST, sendBytes);
                } else {
                    future = future.thenCombineAsync(mockConnect.send(Commands.MAIL_DEL_BATCH_CONST, sendBytes), new BiFunction<Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer integer, Integer integer2) {
                            return integer + integer2;
                        }
                    }, mockConnect.executor());
                }
            }
            if (future != null) {
                future.whenCompleteAsync((r, e) -> {
                    mail_data.mailState = Do;
                }, mockConnect.executor());
            } else {
                mail_data.mailState = Do;
            }
        }
    }, Rqst,

    ;

    public void execute(MockConnect mockConnect) {

    }
}
