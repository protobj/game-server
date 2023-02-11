package io.protobj.microserver.ans;

import java.util.List;

import com.guangyu.cd003.projects.message.common.msg.CrossSvrMsg;
import com.pv.common.utilities.common.CommonUtil;
import com.pv.framework.gs.core.module.msgproc.IRespMsg;

public class AnsFullLst implements IRespMsg, CrossSvrMsg {

    private List<CrossSvrMsg> answers;

    public AnsFullLst() {
    }

    public AnsFullLst(List<CrossSvrMsg> answers) {
        this.answers = answers;
    }

    public List<CrossSvrMsg> getAnswers() {
        return answers;
    }

    public void setAnswers(List<CrossSvrMsg> answers) {
        this.answers = answers;
    }

    public void addAnswer(CrossSvrMsg answer) {
        if (answers == null) {
            answers = CommonUtil.createList();
        }
        answers.add(answer);
    }

    public <T> T answerAt(int ix) {
        return answers.get(ix).as();
    }

    public <T extends CrossSvrMsg> T answerAt(int ix, Class<T> clz) {
        return answers.get(ix).as(clz);
    }
    
    
    @SuppressWarnings("unchecked")
	public <T extends CrossSvrMsg> T answerAt(Class<T> clz) {
    	for (CrossSvrMsg crossSvrMsg : answers) {
			if (clz.isInstance(crossSvrMsg)) {
				return ((T) crossSvrMsg);
			}
		}
        return null;
    }
    

}
