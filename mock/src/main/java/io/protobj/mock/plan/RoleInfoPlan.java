//package io.protobj.mock.plan;
//
//import io.reactivex.rxjava3.core.Observable;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//import static com.guangyu.cd003.projects.common.cons.Commands.*;
//
//public class RoleInfoPlan extends Plan {
//    @Override
//    protected Observable<Integer> execute0(MockConnect connect) {
//        List<CompletableFuture<Integer>> futureList = new ArrayList<>();
//        futureList.add(connect.send(ROLE_SYSTIME_QRY_CONST, null));//请求系统时间
//        futureList.add(connect.send(DEPOT_LOAD_CONST, null));//请求背包数据
//        futureList.add(connect.send(CITY_LOAD_CONST, null));//加载个人城市数据
//        futureList.add(connect.send(ROLEINFOSTAT_QUERY_TOTAL_CONST, null));//玩家总战力
//        futureList.add(connect.send(HERO_LOAD_CONST, null));//加载英雄数据
//        futureList.add(connect.send(UAV_LOAD_CONST, null));//加载无人机数据
//        futureList.add(connect.send(LEGION_LOAD_CONST, null));//加载军团
////                    futureList.add(connect.send(RECONNOITRE_INFO_CONST, null));//侦察营地信息
//        futureList.add(connect.send(SHADE_INFO_CONST, null));//获取遮罩信息
//        futureList.add(connect.send(MAIL_GET_ALL_CONST, null));//获取邮件列表信息
////                    {
////                        RqstCbtRptSumrsesQry rqst = new RqstCbtRptSumrsesQry();
////                        rqst.fromIx = 0;
////                        rqst.num = 20;
////                        futureList.add(connect.send(CBT_RPT_SUMRSES_QRY_CONST, rqst));//查询战报集 列表
////                    }
//        futureList.add(connect.send(HEROOCPANCY_INFO_CONST, null));//驻扎信息
//        futureList.add(connect.send(TALENT_ROLE_INFO_CONST, null));//玩家天赋信息
//        futureList.add(connect.send(EXPDN_GET_REC_INFO_CONST, null));//获取战役记录信息
//        futureList.add(connect.send(GUIDE_INFO_CONST, null));//请求新手引导数据
//        futureList.add(connect.send(QST_GET_INFO_CONST, null));//获取任务信息(无活动任务信息)
//        futureList.add(connect.send(PRODUCT_LOAD_CONST, null));//获取生产信息
//        futureList.add(connect.send(BARRACKS_LOAD_CONST, null));//获取兵营信息
//        futureList.add(connect.send(HOSPITAL_LOAD_CONST, null));//获取医院信息
//        futureList.add(connect.send(GUARD_TOWER_LOAD_CONST, null));//查询守卫塔信息
////                    {
////
////                        RqstSceneElemt rqst = new RqstSceneElemt();
////                        futures[20] = connect.send(SCENE_RQST_ELEMT_CONST, rqst);//请求场景元素
////                    }
////                    {
////                        RqstGetChatCache rqstChat = new RqstGetChatCache();
////                        rqstChat.channel = -1;
////                        futureList.add(connect.send(CHAT_GET_CACHE_CHAT_CONST, rqstChat));//获取聊天信息  channel:-1->私聊频道, 10->世界频道,
////                    }
//        {
//            RqstGetChatCache rqstChat10 = new RqstGetChatCache();
//            rqstChat10.channel = 10;
//            futureList.add(connect.send(CHAT_GET_CACHE_CHAT_CONST, rqstChat10));//获取聊天信息  channel:-1->私聊频道, 10->世界频道,
//        }
//        CompletableFuture<Integer>[] completableFutures = futureList.toArray(new CompletableFuture[0]);
//        CompletableFuture<Void> stage = CompletableFuture.allOf(completableFutures);
//        return Observable.fromCompletionStage(stage.thenApply((vo) -> 0));
//    }
//}
