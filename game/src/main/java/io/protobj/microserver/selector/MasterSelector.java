package io.protobj.microserver.selector;

import com.guangyu.cd003.projects.message.core.SvrType;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于Curator/Zookeeper进行主节点选举
 *
 * @author ZhaoZhe (joezane.cn@gmail.com)
 * @date 2022/3/23 11:23
 */
public class MasterSelector {
    private static final Logger logger = LoggerFactory.getLogger(MasterSelector.class);
    private static final String BASE_PATH = "master";

    private final LeaderLatch leaderLatch;

    public MasterSelector(CuratorFramework curatorFramework, SvrType svrType, String id, Runnable winMasterRun, Runnable loseMasterRun) {
        String path = "/" + BASE_PATH + "/" + svrType.name();
        leaderLatch = new LeaderLatch(curatorFramework, path);
        leaderLatch.addListener(new LeaderLatchListener() {
            @Override
            public void isLeader() {
                logger.info("当前节点选举成为主节点, path = {}, id = {}", curatorFramework.getNamespace() + path, id);
                winMasterRun.run();
            }

            @Override
            public void notLeader() {
                logger.info("当前节点丧失主节点资格, path = {}, id = {}", curatorFramework.getNamespace() + path, id);
                loseMasterRun.run();
            }
        });
    }

    public void select() throws Exception {
        this.leaderLatch.start();
    }
}
