package io.protobj.cluster.message;

import io.protobj.cluster.ClusterContext;

public interface MessageHandler {
    void handle(ClusterContext context, Message message);

}
