package io.protobj.services.message;

import io.protobj.services.ClusterContext;
import io.protobj.services.api.Message;

public interface MessageHandler {
    void handle(ClusterContext context, Message message);

}
