package org.protobj.mock.plan;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.common.module.league.msg.RqstApplyJoinLeague;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import io.reactivex.rxjava3.core.Observable;

public class JoinLeaguePlan extends Plan {
    @Override
    protected Observable<Integer> execute0(MockConnect connect) {
        RqstApplyJoinLeague rqstApplyJoinLeague = new RqstApplyJoinLeague();
        rqstApplyJoinLeague.leagueId = 312963950834618368L;
        connect.send(Commands.LEAGUE_JOIN_CONST, rqstApplyJoinLeague);
        return Observable.empty();
    }
}
