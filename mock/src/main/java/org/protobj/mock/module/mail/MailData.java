package org.protobj.mock.module.mail;

import com.guangyu.cd003.projects.gs.module.mail.cons.ConstMail;
import com.guangyu.cd003.projects.gs.module.mail.msg.RespMail;
import com.guangyu.cd003.projects.gs.module.mail.msg.RespMailInfo;
import com.guangyu.cd003.projects.gs.module.mail.msg.RespMailOp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MailData {

    public MailState mailState = MailState.Do;

    RespMailInfo respMailInfo;

    //type
    public Set<Integer> canDeleteList = new HashSet<>();
    //type
    public Set<Integer> canRecvList = new HashSet<>();


    public void resetDeleteList() {
        canDeleteList.clear();
        for (RespMail mail : respMailInfo.mails) {
            if (mail.isFlag(ConstMail.FLAG_PICK) || mail.isFlag(ConstMail.FLAG_READ)) {
                canDeleteList.add(mail.type);
            }
        }
    }

    public void resetRecvList() {
        canRecvList.clear();
        for (RespMail mail : respMailInfo.mails) {
            if (mail.accessorys != null && !mail.isFlag(ConstMail.FLAG_PICK)) {
                canRecvList.add(mail.type);
            }
        }
    }




    public void handle(RespMailInfo respMsg) {
        this.respMailInfo = respMsg;
    }

    public void handle(RespMailOp respMailOp) {
        if (CollectionUtils.isNotEmpty(respMailOp.del)) {
            respMailInfo.mails.removeIf(it -> respMailOp.del.contains(it.id));
        }
        if (CollectionUtils.isNotEmpty(respMailOp.add)) {
            List<RespMail> list = respMailInfo.mails;
            if (list != null) {
                list.addAll(respMailOp.add);
            } else {
                respMailInfo.mails = respMailOp.add;
            }
        }
        if (MapUtils.isNotEmpty(respMailOp.gocMailStateUpdMap())) {
            respMailOp.stateUpd.forEach((id, state) -> {
                for (RespMail mail : respMailInfo.mails) {
                    if (mail.id.equals(id)) {
                        mail.flags = state.byteValue();
                        break;
                    }
                }
            });
        }
    }

    public Set<Integer> getRectMail() {
        if (!this.canRecvList.isEmpty()) {
            return this.canRecvList;
        }
        resetRecvList();
        return this.canRecvList;
    }

    public Set<Integer> getDelMail() {
        if (!this.canDeleteList.isEmpty()) {
            return this.canDeleteList;
        }
        resetDeleteList();
        return this.canDeleteList;
    }
}
