package io.protobj;

import io.protobj.util.Jackson;
import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.ClusterConfig;
import io.scalecube.cluster.ClusterImpl;
import io.scalecube.cluster.ClusterMessageHandler;
import io.scalecube.cluster.membership.MembershipEvent;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.net.Address;
import io.scalecube.transport.netty.websocket.WebsocketTransportFactory;
import reactor.core.publisher.Flux;

import java.util.concurrent.locks.LockSupport;

public class Application {
    public static void main(String[] args) throws InterruptedException {
        //异步分线程写日志
//        System.setProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.CustomAsyncLoggerContextSelector");
        // Start cluster node Alice to listen and respond for incoming greeting messages
        ClusterConfig clusterConfig = ClusterConfig.defaultConfig();

        clusterConfig = clusterConfig.membership(t -> t.seedMembers(Address.create("0.0.0.0", 4343)));
        Cluster alice =
                new ClusterImpl(clusterConfig)
                        .config(config -> config.externalPort(4343).externalHost("0.0.0.0").memberAlias("alice"))
                        .config(config -> config.transport(transportConfig -> transportConfig.port(4343)))
                        .transportFactory(WebsocketTransportFactory::new)
                        .handler(
                                cluster -> {
                                    return new ClusterMessageHandler() {
                                        @Override
                                        public void onMessage(Message msg) {
                                            System.out.println("Alice received: " + msg.data());
                                            cluster
                                                    .send(msg.sender(), Message.fromData("Greetings from Alice"))
                                                    .subscribe(null, Throwable::printStackTrace);
                                        }

                                        @Override
                                        public void onMembershipEvent(MembershipEvent event) {
                                            System.out.println(event);
                                        }

                                        @Override
                                        public void onGossip(Message gossip) {
                                            System.out.println("Alice received: " + gossip.data());
                                        }
                                    };
                                })
                        .startAwait();
        // Join cluster node Bob to cluster with Alice, listen and respond for incoming greeting
        // messages
        Cluster bob =
                new ClusterImpl(clusterConfig.clone())
                        .config(config -> config.memberAlias("bob"))
                        .transportFactory(WebsocketTransportFactory::new)
                        .handler(
                                cluster -> {
                                    return new ClusterMessageHandler() {
                                        @Override
                                        public void onMessage(Message msg) {
                                            System.out.println("Bob received: " + msg.data());
                                            cluster
                                                    .send(msg.sender(), Message.fromData("Greetings from Bob"))
                                                    .subscribe(null, Throwable::printStackTrace);
                                        }
                                        @Override
                                        public void onGossip(Message gossip) {
                                            System.out.println("Bob received: " + gossip.data());
                                        }
                                    };
                                })
                        .startAwait();

        // Join cluster node Carol to cluster with Alice and Bob
        Cluster carol =
                new ClusterImpl(clusterConfig.clone())
                        .config(config -> config.memberAlias("carol"))
                        .transportFactory(WebsocketTransportFactory::new)
                        .handler(
                                cluster -> {
                                    return new ClusterMessageHandler() {
                                        @Override
                                        public void onMessage(Message msg) {
                                            System.out.println("Carol received: " + msg.data());
                                        }
                                        @Override
                                        public void onGossip(Message gossip) {
                                            System.out.println("Carol received: " + gossip.data());
                                        }
                                    };
                                })
                        .startAwait();

        // Send from Carol greeting message to all other cluster members (which is Alice and Bob)
        Message greetingMsg = Message.fromData("Greetings from Carol");
        Flux.fromIterable(carol.otherMembers())
                .flatMap(member -> carol.send(member, greetingMsg))
                .subscribe(null, Throwable::printStackTrace);
        // Avoid exit main thread immediately ]:->
        System.err.println(Jackson.INSTANCE.encode(ClusterConfig.defaultConfig()));
        alice.updateMetadata("hahahaha").subscribe();
        alice.requestResponse(carol.member(), Message.fromData("hello requestResponse"))
                .subscribe(null, Throwable::printStackTrace);

        LockSupport.park();

    }
}