package org.protobj.mock.module.league;

public enum LeagueJoinOrCreState {
    InitCheck,//初始检查
    tryJoin,//尝试加入
    tryJoinRqsting,//没有加入时发送请求
    tryCre,//尝试创建
    tryCreRqsting,
    Joined,//已加入
    GetInfoRqsting,//获取info发送请求
    InfoGeted,//
    ;
}
