package org.protobj.mock.module.league;

import com.guangyu.cd003.projects.common.module.league.msg.RespLeague;
import com.guangyu.cd003.projects.common.module.league.msg.RespLeagueDisbandUpdate;
import com.guangyu.cd003.projects.gs.module.leaguebuilding.msg.RespLeagueOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;

public class LeagueData {

    LeagueJoinOrCreState joinOrCreState = LeagueJoinOrCreState.InitCheck;

    RespLeague respLeague;

    RespLeagueOp respLeagueOp;


    public LeagueJoinOrCreState getJoinOrCreState() {
        return joinOrCreState;
    }

    public void setJoinOrCreState(LeagueJoinOrCreState joinOrCreState) {
        this.joinOrCreState = joinOrCreState;
    }


    public void handle(RespLeagueOp respLeagueOp) {
        if (respLeagueOp.respCreateLeague != null) {
            respLeague = respLeagueOp.respCreateLeague.respLeague;
        }
        if (respLeagueOp.respLeagueInfo != null) {
            respLeague = respLeagueOp.respLeagueInfo.respLeague;
        }
        this.respLeagueOp = respLeagueOp;
    }

    public void handle(MockConnect connect, RespLeagueDisbandUpdate respMsg) {
        connect.ROLE_DATA.respRoleInfo.roleInfo.leagueId = 0;
        setJoinOrCreState(LeagueJoinOrCreState.InitCheck);
        this.respLeagueOp = null;
        this.respLeague = null;
    }
}
