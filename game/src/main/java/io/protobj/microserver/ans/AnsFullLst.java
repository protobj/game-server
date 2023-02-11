package io.protobj.microserver.ans;

import java.util.ArrayList;
import java.util.List;


public class AnsFullLst {

    private List<Object> answers;

    public AnsFullLst() {
    }

    public AnsFullLst(List<Object> answers) {
        this.answers = answers;
    }

    public List<Object> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Object> answers) {
        this.answers = answers;
    }

    public void addAnswer(Object answer) {
        if (answers == null) {
            answers = new ArrayList<>();
        }
        answers.add(answer);
    }

    public <T> T answerAt(int ix) {
        return (T) answers.get(ix);
    }

    public <T> T answerAt(int ix, Class<T> clz) {
        return clz.cast(answers.get(ix));
    }


    @SuppressWarnings("unchecked")
    public <T> T answerAt(Class<T> clz) {
        for (Object crossSvrMsg : answers) {
            if (clz.isInstance(crossSvrMsg)) {
                return ((T) crossSvrMsg);
            }
        }
        return null;
    }


}
